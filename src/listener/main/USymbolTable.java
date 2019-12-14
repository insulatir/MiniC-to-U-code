package listener.main;

import java.util.HashMap;
import java.util.Map;

import generated.MiniCParser;
import generated.MiniCParser.Fun_declContext;
import generated.MiniCParser.Local_declContext;
import generated.MiniCParser.Var_declContext;
import static listener.main.UCodeGenListenerHelper.*;

public class USymbolTable {
	enum Type {
		INT, INTARRAY, VOID, ERROR
	}
	
	static public class VarInfo {
		Type type; 
		int block;
		int id;
		int initVal;
		int size;
		
		public VarInfo(Type type, int block, int id, int size, int initVal) {
			this.type = type;
			this.block = block;
			this.id = id;
			this.size = size;
			this.initVal = initVal;
		}
		public VarInfo(Type type, int block, int id, int size) {
			this.type = type;
			this.block = block;
			this.id = id;
			this.size = size;
			this.initVal = 0;
		}
	}
	
	static public class FInfo {
		public String sigStr;
		public int block;
		public int local;
		public boolean hasArray;
	}
	
	private Map<String, VarInfo> _lsymtable = new HashMap<>();	// local v.
	private Map<String, VarInfo> _gsymtable = new HashMap<>();	// global v.
	private Map<String, FInfo> _fsymtable = new HashMap<>();	// function 
	
		
	private int _block = 0;
	private int _globalVarID = 0;
	private int _localVarID = 0;
	private int _labelID = 0;
	private int _tempVarID = 32;
	
	USymbolTable(){
		initFunTable();
	}
	
	void initFunDecl(){		// at each func decl
		_lsymtable.clear();
		// 함수 집입마다 블럭 번호 증가
		_block++;
		_localVarID = 0;
		_tempVarID = 32;		
	}
	
	int getGlobalCount() {
		return _globalVarID;
	}
	
	VarInfo getVarInfo(String varname) {
		VarInfo varInfo;
		
		varInfo = _lsymtable.get(varname);
		if (varInfo != null) {
			return varInfo;
		}
		
		varInfo = _gsymtable.get(varname);
		if (varInfo != null) {
			return varInfo;
		}
		
		return null;
	}
	
	void putLocalVar(String varname, Type type){
		// type, id를 가지고 변수정보 생성
		VarInfo varinfo = new VarInfo(type, _block, _localVarID++, 1);
		// 지역변수테이블에 변수이름과 변수정보를 쌍으로 하여 추가
		_lsymtable.put(varname, varinfo);
	}
	
	void putGlobalVar(String varname, Type type){
		// type, id를 가지고 변수정보 생성
		VarInfo varinfo = new VarInfo(type, _block, _globalVarID++, 1);
		// 전역변수테이블에 변수이름과 변수정보를 쌍으로 하여 추가
		_gsymtable.put(varname, varinfo);
	}
	
	void putLocalVar(String varname, int size, Type type){
		// type, id를 가지고 변수정보 생성
		VarInfo varinfo = new VarInfo(type, _block, _localVarID, size);
		// 지역변수테이블에 변수이름과 변수정보를 쌍으로 하여 추가
		_lsymtable.put(varname, varinfo);
		_localVarID += size;
	}
	
	void putGlobalVar(String varname, int size, Type type){
		// type, id를 가지고 변수정보 생성
		VarInfo varinfo = new VarInfo(type, _block, _globalVarID, size);
		// 전역변수테이블에 변수이름과 변수정보를 쌍으로 하여 추가
		_gsymtable.put(varname, varinfo);
		_globalVarID += size;
	}
	
	void putLocalVarWithInitVal(String varname, Type type, int initVar){
		// type, id와 초기값을 가지고 변수정보 생성
		VarInfo varinfo = new VarInfo(type, _block, _localVarID++, initVar);
		// 지역변수테이블에 변수이름과 변수정보를 쌍으로 하여 추가
		_lsymtable.put(varname, varinfo);
	}
	void putGlobalVarWithInitVal(String varname, Type type, int initVar){
		// type, id와 초기값을 가지고 변수정보 생성
		VarInfo varinfo = new VarInfo(type, _block, _globalVarID++, initVar);
		// 전역변수테이블에 변수이름과 변수정보를 쌍으로 하여 추가
		_gsymtable.put(varname, varinfo);
	}
	
	void putParams(MiniCParser.ParamsContext params) {
		for(int i = 0; i < params.param().size(); i++) {
			// 인자 이름 
			String pname = getParamName(params.param(i));
			// 인자 타입
			Type type = Type.valueOf(params.param(i).type_spec().getText().toUpperCase());
			// 배열 타입
			if (isArrayParamDecl(params.param(i))) {
				type = Type.valueOf("INTARRAY");
			}
			// 지역변수테이블에 인자 이름, 인자 타입을 쌍으로 하여 추가
			putLocalVar(pname, type);
		}
	}
	
	private void initFunTable() {
		// println 함수정보
		FInfo printlninfo = new FInfo();
		printlninfo.sigStr = "write";
		// 함수테이블에 println은 "write"라는 이름을 가지게 하여 추가
		_fsymtable.put("write", printlninfo);
		
		// main 함수정보
		FInfo maininfo = new FInfo();
		printlninfo.sigStr = "main";
		// 함수테이블에 main은 "main"라는 이름을 가지게 하여 추가
		_fsymtable.put("main", maininfo);
	}
	
	boolean hasArray(MiniCParser.ParamsContext ctx) {
		for (int i = 0; i < ctx.param().size(); i++) {
			if (isArrayParamDecl(ctx.param(i))) {
				return true;
			}
		}
		return false;
	}
	
	// 함수 정보
	FInfo getFunInfo(String fname) {
		return _fsymtable.get(fname);
	}
	
	// 함수테이블에 함수정보 저장
	String putFunInfo(MiniCParser.Fun_declContext ctx) {
		FInfo finfo = new FInfo();
		String fname = "";
		
		fname = ctx.IDENT().getText();
		finfo.sigStr = fname;
		finfo.block = _block;
		finfo.local = _localVarID;
		finfo.hasArray = hasArray(ctx.params());
		
		_fsymtable.put(fname, finfo);
		
		return fname;
	}
	
	String getVarId(String name){
		// 이름을 가지고 지역변수테이블에서 지역변수정보 탐색
		VarInfo lvar = (VarInfo) _lsymtable.get(name);
		// 지역변수정보가 존재한다면
		if (lvar != null) {
			// 지역변수의 id 반환
			return lvar.id+"";
		}
		
		// 이름을 가지고 전역변수테이블에서 전역변수정보 탐색
		VarInfo gvar = (VarInfo) _gsymtable.get(name);
		// 전역변수정보가 존재한다면
		if (gvar != null) {
			// 전역변수의 id 반환
			return gvar.id+"";
		}
		
		// 두 테이블 모두에서 존재하지 않는다면 ERROR
		return Type.ERROR+"";
	}
	
	Type getVarType(String name){
		// 이름을 가지고 지역변수테이블에서 지역변수정보 탐색
		VarInfo lvar = (VarInfo) _lsymtable.get(name);
		// 지역변수정보가 존재한다면
		if (lvar != null) {
			// 지역변수의 타입 반환
			return lvar.type;
		}
		
		// 이름을 가지고 전역변수테이블에서 전역변수정보 탐색
		VarInfo gvar = (VarInfo) _gsymtable.get(name);
		// 전역변수정보가 존재한다면
		if (gvar != null) {
			// 전역변수의 타입 반환
			return gvar.type;
		}
		
		// 두 테이블 모두에서 존재하지 않는다면 ERROR
		return Type.ERROR;	
	}
	String newLabel() {
		// 새로운 라벨 생성 후 _labelID 증가
		return "$$" + _labelID++;
	}
	
	String newTempVar() {
		String id = "";
		// 새로운 임시변수 생성 후 _tempVarID 감소
		return id + _tempVarID--;
	}

	// global
	public String getVarId(Var_declContext ctx) {
		String sname = "";
		// 전역변수의 이름을 가지고 전역변수의 id 획득
		sname += getVarId(ctx.IDENT().getText());
		return sname;
	}

	// local
	public String getVarId(Local_declContext ctx) {
		String sname = "";
		// 지역변수의 이름을 가지고 지역변수의 id 획득
		sname += getVarId(ctx.IDENT().getText());
		return sname;
	}
}
