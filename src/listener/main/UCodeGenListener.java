package listener.main;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.antlr.v4.runtime.tree.ParseTreeListener;
import org.antlr.v4.runtime.tree.ParseTreeProperty;

import generated.MiniCBaseListener;
import generated.MiniCParser;
import generated.MiniCParser.ParamsContext;
import static listener.main.UCodeGenListenerHelper.*;
import static listener.main.USymbolTable.*;

public class UCodeGenListener extends MiniCBaseListener implements ParseTreeListener {
	ParseTreeProperty<String> newTexts = new ParseTreeProperty<String>();
	USymbolTable symbolTable = new USymbolTable();
	
	int tab = 0;
	int label = 0;
	
	// program	: decl+
	
	@Override
	public void enterFun_decl(MiniCParser.Fun_declContext ctx) {
		// 심볼테이블 초기화
		symbolTable.initFunDecl();
		
		ParamsContext params;
		
		params = (MiniCParser.ParamsContext) ctx.getChild(3);
		// 심볼테이블에 인자들 추가
		symbolTable.putParams(params);	
	}
	
	// var_decl	: type_spec IDENT ';' | type_spec IDENT '=' LITERAL ';'| type_spec IDENT '[' LITERAL ']' ';'
	@Override
	public void enterVar_decl(MiniCParser.Var_declContext ctx) {
		String varName = ctx.IDENT().getText();
		
		// 전역변수가 배열 타입
		if (isArrayDecl(ctx)) {
			symbolTable.putGlobalVar(varName, Type.INTARRAY);
		}
		// 전역변수가 초기값을 가짐
		else if (isDeclWithInit(ctx)) {
			symbolTable.putGlobalVarWithInitVal(varName, Type.INT, initVal(ctx));
		}
		else  { // simple decl
			symbolTable.putGlobalVar(varName, Type.INT);
		}
	}
	
	@Override
	public void enterLocal_decl(MiniCParser.Local_declContext ctx) {
		// 지역변수가 배열 타입
		if (isArrayDecl(ctx)) {
			symbolTable.putLocalVar(getLocalVarName(ctx), Type.INTARRAY);
		}
		// 지역변수가 초기값을 가짐
		else if (isDeclWithInit(ctx)) {
			symbolTable.putLocalVarWithInitVal(getLocalVarName(ctx), Type.INT, initVal(ctx));	
		}
		else  { // simple decl
			symbolTable.putLocalVar(getLocalVarName(ctx), Type.INT);
		}	
	}
	
	@Override
	public void exitProgram(MiniCParser.ProgramContext ctx) {
		// 공통의 초기 상태
		String classProlog = getFunProlog();
		
		String fun_decl = "", var_decl = "";
		
		String program = "";
		
		for(int i = 0; i < ctx.getChildCount(); i++) {
			if(isFunDecl(ctx, i))
				// 함수 decl
				fun_decl += newTexts.get(ctx.decl(i));
			else
				// 변수 decl
				var_decl += newTexts.get(ctx.decl(i));
		}
		
		// 최종 문장
		program = classProlog + var_decl + fun_decl;
		
		// 빈 줄 삭제
		program = deleteEmptyLines(program);
		
		// 트리에 최종 문장 추가
		newTexts.put(ctx, program);
		
//		// 'Test.j' 파일
//		File file = new File("Test.j");
//		
//		try {
//			FileWriter fw = new FileWriter(file);
//			// 'Test.j' 파일에 program 쓰기
//			fw.write(newTexts.get(ctx));
//			fw.close();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
		
		// program 출력
		System.out.println(newTexts.get(ctx));
	}	
	
	// decl	: var_decl | fun_decl
	@Override
	public void exitDecl(MiniCParser.DeclContext ctx) {
		String decl = "";
		if(ctx.getChildCount() == 1)
		{
			if(ctx.var_decl() != null)		// var_decl
				decl += newTexts.get(ctx.var_decl());
			else							// fun_decl
				decl += newTexts.get(ctx.fun_decl());
		}
		newTexts.put(ctx, decl);
	}
	
	// stmt	: expr_stmt | compound_stmt | if_stmt | while_stmt | return_stmt
	@Override
	public void exitStmt(MiniCParser.StmtContext ctx) {
		String stmt = "";
		if(ctx.getChildCount() > 0)
		{
			if(ctx.expr_stmt() != null)				// expr_stmt
				stmt += newTexts.get(ctx.expr_stmt());
			else if(ctx.compound_stmt() != null)	// compound_stmt
				stmt += newTexts.get(ctx.compound_stmt());
			else if (ctx.if_stmt() != null)			// if_stmt
				stmt += newTexts.get(ctx.if_stmt());
			else if (ctx.while_stmt() != null)		// while_stmt
				stmt += newTexts.get(ctx.while_stmt());
			else if (ctx.return_stmt() != null)		// return_stmt
				stmt += newTexts.get(ctx.return_stmt());
		}
		newTexts.put(ctx, stmt);
	}
	
	// expr_stmt : expr ';'
	@Override
	public void exitExpr_stmt(MiniCParser.Expr_stmtContext ctx) {
		String stmt = "";
		if(ctx.getChildCount() == 2)
		{
			stmt += newTexts.get(ctx.expr());	// expr
		}
		newTexts.put(ctx, stmt);
	}
	
	// while_stmt : WHILE '(' expr ')' stmt
	@Override
	public void exitWhile_stmt(MiniCParser.While_stmtContext ctx) {
		String stmt = "";
		// while 조건문
		String condExpr = newTexts.get(ctx.expr());
		// while 실행문
		String thenStmt = newTexts.get(ctx.stmt());
		
		// while 라벨
		String lwhile = symbolTable.newLabel();
		// end 라벨
		String lend = symbolTable.newLabel();
		
		stmt = lwhile + "\t" + "nop" + "\n"
				+ condExpr + "\n"
				+ "\t" + "fjp " + lend + "\n"
				+ thenStmt + "\n"
				+ "\t" + "ujp " + lwhile + "\n"
				+ lend + "\t" + "nop" + "\n";
		
		newTexts.put(ctx, stmt);
	}
	
	// fun_decl	: type_spec IDENT '(' params ')' compound_stmt
	@Override
	public void exitFun_decl(MiniCParser.Fun_declContext ctx) {
		String decl = "";
		// 함수 이름
		String fname = getFunName(ctx);
		
		// 함수 시작 부분
		decl += funcHeader(ctx, fname);
		// 함수 내부 문장들
		decl += newTexts.get(ctx.compound_stmt());
		// 리턴 존재 여부 판별
		decl += noReturn(ctx);
		// 함수의 끝
		decl += "\t" + "end" + "\n";
		
		newTexts.put(ctx, decl);
	}

	// 함수 시작 부분
	private String funcHeader(MiniCParser.Fun_declContext ctx, String fname) {
		String header = "";
		FInfo fInfo = symbolTable.getFunInfo(fname);
		int i = 0;
		while (ctx.params().param(i) != null) {
			String varname = ctx.params().param(i).IDENT().getText();
			VarInfo vInfo = symbolTable.getVarInfo(varname);
			header += printSymbol(vInfo);
			i += 1;
		}
		
		header = fname + "\t" + "proc " + fInfo.block + " " + fInfo.local + "\n" + header;	
		return header;
	}
	
	@Override
	public void exitVar_decl(MiniCParser.Var_declContext ctx) {
		// 전역변수 이름
		String varName = ctx.IDENT().getText();
		String varDecl = "";
		VarInfo varInfo = symbolTable.getVarInfo(varName);
		
		varDecl += printSymbol(varInfo);
		
		if (isDeclWithInit(ctx)) {
			varDecl += "putfield " + varName + "\n";  
			// v. initialization => Later! skip now..: 
		}
		newTexts.put(ctx, varDecl);
	}
	
	@Override
	public void exitLocal_decl(MiniCParser.Local_declContext ctx) {
		String varDecl = "";
		VarInfo varInfo = symbolTable.getVarInfo(ctx.IDENT().getText());
		
		varDecl += printSymbol(varInfo);
		
		// 지역변수가 초기값을 가지고 있는 경우
		if (isDeclWithInit(ctx)) {
					// 초기값 스택 추가
			varDecl += "\t" + "ldc " + ctx.LITERAL().getText() + "\n"
					// 지역변수에 스택 값 저장
					+ "\t" + "str " + varInfo.block + " " + varInfo.id + "\n"; 			
		}
		
		newTexts.put(ctx, varDecl);
	}
	
	String printSymbol(VarInfo varInfo) {
		return "\t" + "sym " + varInfo.block + " " + varInfo.id + " " + 1 + "\n"; 
	}
	
	// compound_stmt : '{' local_decl* stmt* '}'
	@Override
	public void exitCompound_stmt(MiniCParser.Compound_stmtContext ctx) {
		String stmt = "";
		
		// 지역변수 추가
		if (ctx.local_decl() != null) {
			for (int i = 0; i < ctx.local_decl().size(); i++) {
				stmt += newTexts.get(ctx.local_decl(i)) + "\n";
			}
		}
		
		// 심볼테이블에 함수 정보 추가 
		// 스택 최적화
		if (ctx.parent instanceof MiniCParser.Fun_declContext) {
			symbolTable.putFunInfo((MiniCParser.Fun_declContext) ctx.parent);
		}
		
		// 문장 추가
		if (ctx.stmt() != null) {
			for (int i = 0; i < ctx.stmt().size(); i++) {
				stmt += newTexts.get(ctx.stmt(i)) + "\n";
			}
		}
		
		newTexts.put(ctx, stmt);
	}

	// if_stmt	: IF '(' expr ')' stmt | IF '(' expr ')' stmt ELSE stmt;
	@Override
	public void exitIf_stmt(MiniCParser.If_stmtContext ctx) {
		String stmt = "";
		// if 조건문
		String condExpr = newTexts.get(ctx.expr());
		// if 실행문
		String thenStmt = newTexts.get(ctx.stmt(0));
		
		// end 라벨
		String lend = symbolTable.newLabel();
		// else 라벨
		String lelse = symbolTable.newLabel();
		
		// else가 없는 경우
		if(noElse(ctx)) {		
			stmt += condExpr + "\n"
				+ "\t" + "fjp " + lend + "\n"
				+ thenStmt + "\n"
				+ lend + "\t" + "nop" + "\n";	
		}
		// else가 있는 경우
		else {
			// else 실행문
			String elseStmt = newTexts.get(ctx.stmt(1));
			stmt += condExpr + "\n"
					+ "\t" + "fjp " + lelse + "\n"
					+ thenStmt + "\n"
					+ "\t" + "ujp " + lend + "\n"
					+ lelse + "\t" + "nop" + "\n" 
					+ elseStmt + "\n"
					+ lend + "\t" + "nop" + "\n";	
		}
		
		newTexts.put(ctx, stmt);
	}
	
	// return_stmt : RETURN ';' | RETURN expr ';'
	@Override
	public void exitReturn_stmt(MiniCParser.Return_stmtContext ctx) {
		String stmt = "";
		
		// 반환 타입에 상관없이 모두 'ret'
		stmt += "\t" + "ret";
		
		newTexts.put(ctx, stmt);
	}
	
	@Override
	public void exitExpr(MiniCParser.ExprContext ctx) {
		String expr = "";

		if(ctx.getChildCount() <= 0) {
			newTexts.put(ctx, ""); 
			return;
		}		
		
		if(ctx.getChildCount() == 1) { // IDENT | LITERAL
			// IDENT인 경우
			if(ctx.IDENT() != null) {
				String varName = ctx.IDENT().getText();
				VarInfo vInfo = symbolTable.getVarInfo(varName);
				expr += "\t" + "lod " + vInfo.block + " " + vInfo.id + " \n";
				//else	// Type int array => Later! skip now..
				//	expr += "           lda " + symbolTable.get(ctx.IDENT().getText()).value + " \n";
			// LITERAL인 경우
			} else if (ctx.LITERAL() != null) {
					String literalStr = ctx.LITERAL().getText();
					expr += "\t" + "ldc " + literalStr + " \n";
			}
		} else if(ctx.getChildCount() == 2) { // UnaryOperation
			expr = handleUnaryExpr(ctx, expr);			
		}
		else if(ctx.getChildCount() == 3) {	 
			if(ctx.getChild(0).getText().equals("(")) { 		// '(' expr ')'
				expr = newTexts.get(ctx.expr(0));
			} else if(ctx.getChild(1).getText().equals("=")) { 	// IDENT '=' expr
				String varName = ctx.IDENT().getText();
				VarInfo vInfo = symbolTable.getVarInfo(varName);
				expr = newTexts.get(ctx.expr(0)) 
						+ "\t" + "str " + vInfo.block + " " + vInfo.id + " \n";
			} else { 											// binary operation
				expr = handleBinExpr(ctx, expr);
			}
		}
		// IDENT '(' args ')' |  IDENT '[' expr ']'
		else if(ctx.getChildCount() == 4) {
			if(ctx.args() != null){		// function calls
				expr = handleFunCall(ctx, expr);
			} else { // expr
				// Arrays: TODO  
			}
		}
		// IDENT '[' expr ']' '=' expr
		else { // Arrays: TODO			*/
		}
		newTexts.put(ctx, expr);
	}

	private String handleUnaryExpr(MiniCParser.ExprContext ctx, String expr) {
		String varName = ctx.getChild(1).getText();
		VarInfo vInfo = symbolTable.getVarInfo(varName);
		
		expr += newTexts.get(ctx.expr(0));
		switch(ctx.getChild(0).getText()) {
		case "-":
			expr += "\t" + "neg" + "\n"; 
			break;
		case "--":
			expr += "\t" + "dec" + "\n"
					+ "\t" + "str " + vInfo.block + " " + vInfo.id + " \n";
			break;
		case "++":
			expr += "\t" + "inc" + "\n"
					+ "\t" + "str " + vInfo.block + " " + vInfo.id + " \n";
			break;
		case "!":
			expr += "\t" + "not" + "\n";
			break;
		}
		return expr;
	}


	private String handleBinExpr(MiniCParser.ExprContext ctx, String expr) {
		expr += newTexts.get(ctx.expr(0));
		expr += newTexts.get(ctx.expr(1));
		
		switch (ctx.getChild(1).getText()) {
			case "*":		// expr(0) expr(1) imul
				expr += "\t" + "mult" + "\n"; break;
			case "/":		// expr(0) expr(1) idiv
				expr += "\t" + "div" + "\n"; break;
			case "%":		// expr(0) expr(1) irem
				expr += "\t" + "mod" + "\n"; break;
			case "+":		// expr(0) expr(1) iadd
				expr += "\t" + "add" + "\n"; break;
			case "-":		// expr(0) expr(1) isub
				expr += "\t" + "sub" + "\n"; break;
				
			case "==":
				expr += "\t" + "eq" + "\n";
				break;
			case "!=":
				expr += "\t" + "ne" + "\n";
				break;
			case "<=":
				expr += "\t" + "ge" + "\n";
				break;
			case "<":
				expr += "\t" + "lt" + "\n";
				break;

			case ">=":
				expr += "\t" + "ge" + "\n";
				break;

			case ">":
				expr += "\t" + "gt" + "\n";
				break;

			case "and":
				expr += "\t" + "and" + "\n";
				break;
			case "or":
				expr += "\t" + "or" + "\n";
				break;

		}
		return expr;
	}
	private String handleFunCall(MiniCParser.ExprContext ctx, String expr) {
		// 함수 이름
		String fname = getFunName(ctx);		
		
		// '_print'이면 'write'로 변환
		if (fname.equals("_print")) {
			fname = "write";
		}

		// call function
		expr = newTexts.get(ctx.args()) 
		  		+ "\t" + "call " + fname + "\n";
		
		return expr;
	}

	// args	: expr (',' expr)* | ;
	@Override
	public void exitArgs(MiniCParser.ArgsContext ctx) {
		String argsStr = "\n";
		
		for (int i=0; i < ctx.expr().size() ; i++) {
			// 인자 추가
			argsStr += newTexts.get(ctx.expr(i)) ; 
		}		

		newTexts.put(ctx, argsStr);
	}
}
