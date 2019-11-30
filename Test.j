	bgn 0
	ldp
	call main
	end
func	proc 1 2
	ldc 0
	istore_1
	iload_0 
	ldc 100 
	isub 
	ifeq label0
	ldc 0
	goto label1
	label0:
	ldc 1
	label1:
	ifeq label9
	label7:
	iload_1 
	ldc 100 
	isub 
	ifle label2
	ldc 0
	goto label3
	label2:
	ldc 1
	label3:
	ifeq label8
	iload_1 
	ldc 1
	iadd
	istore_1
	goto label7
	label8:
	label9:
	iload_1 
	ireturn
	end
main	proc 2 2
	ldc 100
	istore_1
	iload_1 
	call listener.main.USymbolTable$FInfo@39ed3c8d
	call null
	return
	end
