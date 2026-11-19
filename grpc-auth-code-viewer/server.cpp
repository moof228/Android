#include <algorithm>
#include <functional>
#include <memory>
#include <optional>
#include <stdexcept>
#include <string>
#include <string_view>
#include <utility>
#include <vector>

#include <google/protobuf/descriptor.h>
#include <grpcpp/grpcpp.h>

#include "auth_options.pb.h"
#include "greeter.grpc.pb.h"

namespace
{

constexpr std::string_view kSessionCookie = "__Host-ips_session";

// ---------- cookie -------------------------------------------------------------------------------

std::optional<std::string_view> FindCookie(const grpc::ServerContext& ctx, std::string_view name)
{
    const auto [begin, end] = ctx.client_metadata().equal_range("cookie");
    for (auto it = begin; it != end; ++it)
    {
        std::string_view header{it->second.data(), it->second.size()};
        while (!header.empty())
        {
            const auto separator = header.find(';');
            auto pair            = header.substr(0, separator);
            pair.remove_prefix(std::min(pair.find_first_not_of(' '), pair.size()));
            const auto eq = pair.find('=');
            if (eq != std::string_view::npos && pair.substr(0, eq) == name)
            {
                return pair.substr(eq + 1);
            }
            if (separator == std::string_view::npos)
            {
                break;
            }
            header.remove_prefix(separator + 1);
        }
    }
    return std::nullopt;
}

// ---------- доступ и глобальные middleware -------------------------------------------------------

using Middleware = std::function<grpc::Status(const grpc::ServerContext&)>;

enum class Access
{
    kPublic,
    kPrivate,
};

/// "/helloworld.Greeter/SayHello" → "helloworld.Greeter.SayHello"
std::string ProtoMethodName(std::string_view grpc_method)
{
    if (!grpc_method.empty() && grpc_method.front() == '/')
    {
        grpc_method.remove_prefix(1);
    }
    std::string name{grpc_method};
    const auto slash = name.rfind('/');
    if (slash != std::string::npos)
    {
        name[slash] = '.';
    }
    return name;
}

Access AccessOf(const std::string& full_method)
{
    const auto* method = google::protobuf::DescriptorPool::generated_pool()->FindMethodByName(full_method);
    if (method == nullptr)
    {
        throw std::invalid_argument("unknown gRPC method: " + full_method);
    }
    return method->options().GetExtension(auth::allow_anonymous) ? Access::kPublic : Access::kPrivate;
}

grpc::Status RequireSessionCookie(const grpc::ServerContext& ctx)
{
    const auto cookie = FindCookie(ctx, kSessionCookie);
    if (!cookie || cookie->empty())
    {
        return {grpc::StatusCode::UNAUTHENTICATED, "session cookie is required"};
    }
    return grpc::Status::OK;
}

/// Одна цепочка на весь сервер. Auth смотрит allow_anonymous у текущего метода.
const std::vector<Middleware> g_middlewares = {
    // сюда же логи / метрики
    [](const grpc::ServerContext& ctx) -> grpc::Status {
        if (AccessOf(ProtoMethodName(ctx.method())) == Access::kPrivate)
        {
            return RequireSessionCookie(ctx);
        }
        return grpc::Status::OK;
    },
};

grpc::Status RunMiddlewares(const grpc::ServerContext& ctx)
{
    for (const auto& mw : g_middlewares)
    {
        if (auto status = mw(ctx); !status.ok())
        {
            return status;
        }
    }
    return grpc::Status::OK;
}

// ---------- база вызова --------------------------------------------------------------------------

class CallBase
{
public:
    virtual ~CallBase()           = default;
    virtual void Proceed(bool ok) = 0;
};

// ---------- unary --------------------------------------------------------------------------------

template <typename Request, typename Response>
class UnaryCall final : public CallBase
{
public:
    using Writer  = grpc::ServerAsyncResponseWriter<Response>;
    using Accept  = std::function<void(grpc::ServerContext*, Request*, Writer*, void* tag)>;
    using Handler = std::function<grpc::Status(grpc::ServerContext&, const Request&, Response&)>;

    struct Method
    {
        Accept accept;
        Handler handler;
    };

    static void Listen(std::shared_ptr<const Method> method)
    {
        new UnaryCall(std::move(method));
    }

    void Proceed(bool ok) override
    {
        if (m_state == State::kFinishing || !ok)
        {
            delete this;
            return;
        }

        Listen(m_method);
        m_state = State::kFinishing;

        if (auto status = RunMiddlewares(m_ctx); !status.ok())
        {
            m_writer.FinishWithError(status, this);
            return;
        }

        Response response;
        const auto status = m_method->handler(m_ctx, m_request, response);
        if (status.ok())
        {
            m_writer.Finish(response, status, this);
        }
        else
        {
            m_writer.FinishWithError(status, this);
        }
    }

private:
    enum class State
    {
        kWaiting,
        kFinishing,
    };

    explicit UnaryCall(std::shared_ptr<const Method> method) : m_method(std::move(method)), m_writer(&m_ctx)
    {
        m_method->accept(&m_ctx, &m_request, &m_writer, this);
    }

    std::shared_ptr<const Method> m_method;
    grpc::ServerContext m_ctx;
    Request m_request;
    Writer m_writer;
    State m_state{State::kWaiting};
};

template <typename Request, typename Response>
void ServeUnary(const std::string& full_method,
                typename UnaryCall<Request, Response>::Accept accept,
                typename UnaryCall<Request, Response>::Handler handler)
{
    (void)AccessOf(full_method);  // опечатка в имени — падение при старте
    using Call = UnaryCall<Request, Response>;
    Call::Listen(std::make_shared<const typename Call::Method>(typename Call::Method{
        .accept  = std::move(accept),
        .handler = std::move(handler),
    }));
}

// ---------- server-streaming ---------------------------------------------------------------------

template <typename Request, typename Response>
class ServerStreamCall final : public CallBase
{
public:
    using Writer = grpc::ServerAsyncWriter<Response>;
    using Accept = std::function<void(grpc::ServerContext*, Request*, Writer*, void* tag)>;
    using Handler = std::function<grpc::Status(grpc::ServerContext&, const Request&, std::vector<Response>&)>;

    struct Method
    {
        Accept accept;
        Handler handler;
    };

    static void Listen(std::shared_ptr<const Method> method)
    {
        new ServerStreamCall(std::move(method));
    }

    void Proceed(bool ok) override
    {
        if (!ok && m_state != State::kWaiting)
        {
            delete this;
            return;
        }

        switch (m_state)
        {
        case State::kWaiting:
        {
            Listen(m_method);

            if (auto status = RunMiddlewares(m_ctx); !status.ok())
            {
                m_state = State::kFinishing;
                m_writer.Finish(status, this);
                return;
            }

            auto status = m_method->handler(m_ctx, m_request, m_pending);
            if (!status.ok())
            {
                m_state = State::kFinishing;
                m_writer.Finish(status, this);
                return;
            }
            m_index = 0;
            m_state = State::kWriting;
            WriteNext();
            return;
        }
        case State::kWriting:
            WriteNext();
            return;
        case State::kFinishing:
            delete this;
            return;
        }
    }

private:
    enum class State
    {
        kWaiting,
        kWriting,
        kFinishing,
    };

    explicit ServerStreamCall(std::shared_ptr<const Method> method) : m_method(std::move(method)), m_writer(&m_ctx)
    {
        m_method->accept(&m_ctx, &m_request, &m_writer, this);
    }

    void WriteNext()
    {
        if (m_index < m_pending.size())
        {
            m_writer.Write(m_pending[m_index++], this);
            return;
        }
        m_state = State::kFinishing;
        m_writer.Finish(grpc::Status::OK, this);
    }

    std::shared_ptr<const Method> m_method;
    grpc::ServerContext m_ctx;
    Request m_request;
    Writer m_writer;
    std::vector<Response> m_pending;
    std::size_t m_index{0};
    State m_state{State::kWaiting};
};

template <typename Request, typename Response>
void ServeServerStream(const std::string& full_method,
                       typename ServerStreamCall<Request, Response>::Accept accept,
                       typename ServerStreamCall<Request, Response>::Handler handler)
{
    (void)AccessOf(full_method);
    using Call = ServerStreamCall<Request, Response>;
    Call::Listen(std::make_shared<const typename Call::Method>(typename Call::Method{
        .accept  = std::move(accept),
        .handler = std::move(handler),
    }));
}

// ---------- client-streaming ---------------------------------------------------------------------

template <typename Request, typename Response>
class ClientStreamCall final : public CallBase
{
public:
    using Reader  = grpc::ServerAsyncReader<Response, Request>;
    using Accept  = std::function<void(grpc::ServerContext*, Reader*, void* tag)>;
    using Handler = std::function<grpc::Status(grpc::ServerContext&, const std::vector<Request>&, Response&)>;

    struct Method
    {
        Accept accept;
        Handler handler;
    };

    static void Listen(std::shared_ptr<const Method> method)
    {
        new ClientStreamCall(std::move(method));
    }

    void Proceed(bool ok) override
    {
        switch (m_state)
        {
        case State::kWaiting:
        {
            if (!ok)
            {
                delete this;
                return;
            }
            Listen(m_method);

            if (auto status = RunMiddlewares(m_ctx); !status.ok())
            {
                m_state = State::kFinishing;
                m_reader.FinishWithError(status, this);
                return;
            }

            m_state = State::kReading;
            m_reader.Read(&m_incoming, this);
            return;
        }
        case State::kReading:
        {
            if (ok)
            {
                m_requests.push_back(std::move(m_incoming));
                m_reader.Read(&m_incoming, this);
                return;
            }
            Response response;
            const auto status = m_method->handler(m_ctx, m_requests, response);
            m_state           = State::kFinishing;
            if (status.ok())
            {
                m_reader.Finish(response, status, this);
            }
            else
            {
                m_reader.FinishWithError(status, this);
            }
            return;
        }
        case State::kFinishing:
            delete this;
            return;
        }
    }

private:
    enum class State
    {
        kWaiting,
        kReading,
        kFinishing,
    };

    explicit ClientStreamCall(std::shared_ptr<const Method> method) : m_method(std::move(method)), m_reader(&m_ctx)
    {
        m_method->accept(&m_ctx, &m_reader, this);
    }

    std::shared_ptr<const Method> m_method;
    grpc::ServerContext m_ctx;
    Reader m_reader;
    Request m_incoming;
    std::vector<Request> m_requests;
    State m_state{State::kWaiting};
};

template <typename Request, typename Response>
void ServeClientStream(const std::string& full_method,
                       typename ClientStreamCall<Request, Response>::Accept accept,
                       typename ClientStreamCall<Request, Response>::Handler handler)
{
    (void)AccessOf(full_method);
    using Call = ClientStreamCall<Request, Response>;
    Call::Listen(std::make_shared<const typename Call::Method>(typename Call::Method{
        .accept  = std::move(accept),
        .handler = std::move(handler),
    }));
}

// ---------- bidi-streaming -----------------------------------------------------------------------

template <typename Request, typename Response>
class BidiStreamCall final : public CallBase
{
public:
    using Stream  = grpc::ServerAsyncReaderWriter<Response, Request>;
    using Accept  = std::function<void(grpc::ServerContext*, Stream*, void* tag)>;
    using Handler = std::function<grpc::Status(grpc::ServerContext&, const Request&, std::vector<Response>&)>;
    using OnDone  = std::function<grpc::Status(grpc::ServerContext&)>;

    struct Method
    {
        Accept accept;
        Handler on_message;
        OnDone on_done = [](grpc::ServerContext&) { return grpc::Status::OK; };
    };

    static void Listen(std::shared_ptr<const Method> method)
    {
        new BidiStreamCall(std::move(method));
    }

    void Proceed(bool ok) override
    {
        if (m_state == State::kFinishing)
        {
            delete this;
            return;
        }

        if (m_state == State::kWaiting)
        {
            if (!ok)
            {
                delete this;
                return;
            }
            Listen(m_method);

            if (auto status = RunMiddlewares(m_ctx); !status.ok())
            {
                m_state = State::kFinishing;
                m_stream.Finish(status, this);
                return;
            }

            m_state = State::kReading;
            m_stream.Read(&m_incoming, this);
            return;
        }

        if (m_state == State::kReading)
        {
            if (!ok)
            {
                const auto status = m_method->on_done(m_ctx);
                m_state           = State::kFinishing;
                m_stream.Finish(status, this);
                return;
            }

            m_pending.clear();
            if (auto status = m_method->on_message(m_ctx, m_incoming, m_pending); !status.ok())
            {
                m_state = State::kFinishing;
                m_stream.Finish(status, this);
                return;
            }
            m_index = 0;
            if (m_pending.empty())
            {
                m_stream.Read(&m_incoming, this);
                return;
            }
            m_state = State::kWriting;
            m_stream.Write(m_pending[m_index++], this);
            return;
        }

        if (m_state == State::kWriting)
        {
            if (!ok)
            {
                delete this;
                return;
            }
            if (m_index < m_pending.size())
            {
                m_stream.Write(m_pending[m_index++], this);
                return;
            }
            m_state = State::kReading;
            m_stream.Read(&m_incoming, this);
        }
    }

private:
    enum class State
    {
        kWaiting,
        kReading,
        kWriting,
        kFinishing,
    };

    explicit BidiStreamCall(std::shared_ptr<const Method> method) : m_method(std::move(method)), m_stream(&m_ctx)
    {
        m_method->accept(&m_ctx, &m_stream, this);
    }

    std::shared_ptr<const Method> m_method;
    grpc::ServerContext m_ctx;
    Stream m_stream;
    Request m_incoming;
    std::vector<Response> m_pending;
    std::size_t m_index{0};
    State m_state{State::kWaiting};
};

template <typename Request, typename Response>
void ServeBidiStream(const std::string& full_method,
                     typename BidiStreamCall<Request, Response>::Accept accept,
                     typename BidiStreamCall<Request, Response>::Handler on_message,
                     typename BidiStreamCall<Request, Response>::OnDone on_done = {})
{
    (void)AccessOf(full_method);
    using Call = BidiStreamCall<Request, Response>;
    typename Call::Method method{
        .accept     = std::move(accept),
        .on_message = std::move(on_message),
    };
    if (on_done)
    {
        method.on_done = std::move(on_done);
    }
    Call::Listen(std::make_shared<const typename Call::Method>(std::move(method)));
}

// ---------- обработчики --------------------------------------------------------------------------

grpc::Status HandleLogin(grpc::ServerContext& ctx, const helloworld::LoginRequest& request, helloworld::LoginReply&)
{
    if (request.username().empty() || request.password().empty())
    {
        return {grpc::StatusCode::UNAUTHENTICATED, "invalid credentials"};
    }
    ctx.AddInitialMetadata(
        "set-cookie",
        std::string{kSessionCookie} + "=demo-token; Path=/; Secure; HttpOnly; SameSite=Strict");
    return grpc::Status::OK;
}

grpc::Status HandleSayHello(grpc::ServerContext&, const helloworld::HelloRequest& request, helloworld::HelloReply& reply)
{
    reply.set_message("Hello " + request.name());
    return grpc::Status::OK;
}

grpc::Status HandleLotsOfReplies(grpc::ServerContext&,
                                 const helloworld::HelloRequest& request,
                                 std::vector<helloworld::HelloReply>& out)
{
    for (int i = 0; i < 3; ++i)
    {
        helloworld::HelloReply reply;
        reply.set_message("Hello " + request.name() + " #" + std::to_string(i));
        out.push_back(std::move(reply));
    }
    return grpc::Status::OK;
}

grpc::Status HandleLotsOfGreetings(grpc::ServerContext&,
                                   const std::vector<helloworld::HelloRequest>& requests,
                                   helloworld::HelloReply& reply)
{
    std::string names;
    for (const auto& r : requests)
    {
        if (!names.empty())
        {
            names += ", ";
        }
        names += r.name();
    }
    reply.set_message("Hello " + names);
    return grpc::Status::OK;
}

grpc::Status HandleBidiHello(grpc::ServerContext&,
                             const helloworld::HelloRequest& request,
                             std::vector<helloworld::HelloReply>& out)
{
    helloworld::HelloReply reply;
    reply.set_message("Hello " + request.name());
    out.push_back(std::move(reply));
    return grpc::Status::OK;
}

}  // namespace

int main()
{
    helloworld::Greeter::AsyncService service;
    grpc::ServerBuilder builder;
    builder.AddListeningPort("0.0.0.0:50051", grpc::InsecureServerCredentials());
    builder.RegisterService(&service);
    auto cq     = builder.AddCompletionQueue();
    auto server = builder.BuildAndStart();
    auto* queue = cq.get();
    const std::string greeter = helloworld::Greeter::service_full_name();

    ServeUnary<helloworld::LoginRequest, helloworld::LoginReply>(
        greeter + ".Login",
        [&service, queue](grpc::ServerContext* ctx, helloworld::LoginRequest* request, auto* writer, void* tag) {
            service.RequestLogin(ctx, request, writer, queue, queue, tag);
        },
        HandleLogin);

    ServeUnary<helloworld::HelloRequest, helloworld::HelloReply>(
        greeter + ".SayHello",
        [&service, queue](grpc::ServerContext* ctx, helloworld::HelloRequest* request, auto* writer, void* tag) {
            service.RequestSayHello(ctx, request, writer, queue, queue, tag);
        },
        HandleSayHello);

    ServeServerStream<helloworld::HelloRequest, helloworld::HelloReply>(
        greeter + ".LotsOfReplies",
        [&service, queue](grpc::ServerContext* ctx, helloworld::HelloRequest* request, auto* writer, void* tag) {
            service.RequestLotsOfReplies(ctx, request, writer, queue, queue, tag);
        },
        HandleLotsOfReplies);

    ServeClientStream<helloworld::HelloRequest, helloworld::HelloReply>(
        greeter + ".LotsOfGreetings",
        [&service, queue](grpc::ServerContext* ctx, auto* reader, void* tag) {
            service.RequestLotsOfGreetings(ctx, reader, queue, queue, tag);
        },
        HandleLotsOfGreetings);

    ServeBidiStream<helloworld::HelloRequest, helloworld::HelloReply>(
        greeter + ".BidiHello",
        [&service, queue](grpc::ServerContext* ctx, auto* stream, void* tag) {
            service.RequestBidiHello(ctx, stream, queue, queue, tag);
        },
        HandleBidiHello);

    void* tag = nullptr;
    bool ok   = false;
    while (queue->Next(&tag, &ok))
    {
        static_cast<CallBase*>(tag)->Proceed(ok);
    }
}
