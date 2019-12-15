package listener.main;

import generated.MiniCParser;
import generated.MiniCParser.ExprContext;
import generated.MiniCParser.Fun_declContext;
import generated.MiniCParser.If_stmtContext;
import generated.MiniCParser.Local_declContext;
import generated.MiniCParser.ParamContext;
import generated.MiniCParser.ParamsContext;
import generated.MiniCParser.Type_specContext;
import generated.MiniCParser.Var_declContext;

public class UCodeGenListenerHelper {
	// 함수인지 판별
	static boolean isFunDecl(MiniCParser.ProgramContext ctx, int i) {
		return ctx.getChild(i).getChild(0) instanceof MiniCParser.Fun_declContext;
	}
	
	// type_spec IDENT '[' ']'
	// 인자가 배열 타입인지 판별
	static boolean isArrayParamDecl(ParamContext param) {
		return param.getChildCount() == 4;
	}
	
	// global vars
	static int initVal(Var_declContext ctx) {
		return Integer.parseInt(ctx.LITERAL().getText());
	}

	// var_decl	: type_spec IDENT '=' LITERAL ';'
	static boolean isDeclWithInit(Var_declContext ctx) {
		return ctx.getChildCount() == 5 ;
	}
	// var_decl	: type_spec IDENT '[' LITERAL ']' ';'
	static boolean isArrayDecl(Var_declContext ctx) {
		return ctx.getChildCount() == 6;
	}

	// local vars
	static int initVal(Local_declContext ctx) {
		return Integer.parseInt(ctx.LITERAL().getText());
	}
	
	// local_decl : type_spec IDENT '[' LITERAL ']' ';'
	static boolean isArrayDecl(Local_declContext ctx) {
		return ctx.getChildCount() == 6;
	}
	// local_decl : type_spec IDENT '=' LITERAL ';'
	static boolean isDeclWithInit(Local_declContext ctx) {
		return ctx.getChildCount() == 5 ;
	}
	
	// params
	static String getParamName(ParamContext param) {
		// 인자 이름
		return param.IDENT().getText();
	}
	
	static String getLocalVarName(Local_declContext local_decl) {
		// 지역변수 이름
		return local_decl.IDENT().getText();
	}
	
	// fun_decl	: type_spec IDENT '(' params ')' compound_stmt
	static String getFunName(Fun_declContext ctx) {
		// 함수 이름
		return ctx.IDENT().getText();
	}
	
	// expr : IDENT '(' args ')'
	static String getFunName(ExprContext ctx) {
		// 함수 이름
		return ctx.IDENT().getText();
	}
	
	// if_stmt : IF '(' expr ')' stmt
	static boolean noElse(If_stmtContext ctx) {
		// 자식의 수가 5개 이하이면 else가 없는 if문
		return ctx.getChildCount() <= 5;
	}
	
	static String noReturn(Fun_declContext ctx) {
		int stmtSize = ctx.compound_stmt().stmt().size();
		// return문이 존재하는 함수
		if (ctx.compound_stmt().stmt(stmtSize-1).return_stmt() != null) {
			// 빈 문장 반환
			return "";
		// return문이 존재하지 않는 함수
		} else {
			// 'return' 반환
			return "\t" + "ret" + "\n";
		}
	}
	
	// 빈 줄 삭제
	static String deleteEmptyLines(String program) {
		StringBuilder res = new StringBuilder("");
		
		String[] lines = program.split("\n");
		for (String line : lines) {
			if (!line.isEmpty()) {
				res.append(line + "\n");
			}
		}
		
		return res.toString();
	}
}
