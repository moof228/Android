#include <stdio.h>

int add(int a, int b) {
    return a + b;
}

int main(void) {
    int x = 40;
    int y = 2;
    int sum = add(x, y);
    printf("sum = %d\n", sum);
    return 0;
}
