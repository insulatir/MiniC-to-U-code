int MAX = 10;

void main(void)
{
	int i;
	int a[10];
	int j;
	i = j = 0;
	while (i < MAX) {
		a[i] = j;
		a[i] = sub(i, a);
		j = j + a[i];
		++i;
	}
	_print(j);
	return;
}

int sub(int i, int a[]) {
	int j;
	
	j = j + a[i];
	return j;
}
