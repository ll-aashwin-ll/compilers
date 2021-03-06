/*
 * Parser.java            
 *
 * This parser for a subset of the VC language is intended to 
 *  demonstrate how to create the AST nodes, including (among others): 
 *  [1] a list (of statements)
 *  [2] a function
 *  [3] a statement (which is an expression statement), 
 *  [4] a unary expression
 *  [5] a binary expression
 *  [6] terminals (identifiers, integer literals and operators)
 *
 * In addition, it also demonstrates how to use the two methods start 
 * and finish to determine the position information for the start and 
 * end of a construct (known as a phrase) corresponding an AST node.
 *
 * NOTE THAT THE POSITION INFORMATION WILL NOT BE MARKED. HOWEVER, IT CAN BE
 * USEFUL TO DEBUG YOUR IMPLEMENTATION.
 *
 * (09-|-April-|-2016)


program       -> func-decl
func-decl     -> type identifier "(" ")" compound-stmt
type          -> void
identifier    -> ID
// statements
compound-stmt -> "{" stmt* "}" 
stmt          -> expr-stmt
expr-stmt     -> expr? ";"
// expressions 
expr                -> additive-expr
additive-expr       -> multiplicative-expr
                    |  additive-expr "+" multiplicative-expr
                    |  additive-expr "-" multiplicative-expr
multiplicative-expr -> unary-expr
	            |  multiplicative-expr "*" unary-expr
	            |  multiplicative-expr "/" unary-expr
unary-expr          -> "-" unary-expr
		    |  primary-expr

primary-expr        -> identifier
 		    |  INTLITERAL
		    | "(" expr ")"
 */

package VC.Parser;

import VC.Scanner.Scanner;
import VC.Scanner.SourcePosition;
import VC.Scanner.Token;

import VC.ErrorReporter;
import VC.ASTs.*;

import static VC.Scanner.Token.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


public class Parser {
	private static final Map<String, Integer[]> first;
	static {
		first = new HashMap<String, Integer[]>();
		first.put("var-decl", new Integer[]{VOID, BOOLEAN, INT, FLOAT});
		first.put("stmt", new Integer[]{LCURLY, IF, FOR, WHILE, BREAK, CONTINUE, RETURN, PLUS, MINUS, NOT, ID, LPAREN, INTLITERAL, FLOATLITERAL,
				BOOLEANLITERAL, STRINGLITERAL, SEMICOLON});
	}
	private Scanner scanner;
	private ErrorReporter errorReporter;
	private Token currentToken;
	private SourcePosition previousTokenPosition;
	private SourcePosition dummyPos = new SourcePosition();
	
/*	private Boolean inFunction;
	 true if lock is available 
	private Boolean inFunctionLock = true;
*/
	public Parser (Scanner lexer, ErrorReporter reporter) {
		scanner = lexer;
		errorReporter = reporter;

		previousTokenPosition = new SourcePosition();

		currentToken = scanner.getToken();
	}

	boolean currentInFirst(String s) {
		return Arrays.asList(first.get(s)).contains(currentToken.kind);
	}

	// match checks to see f the current token matches tokenExpected.
	// If so, fetches the next token.
	// If not, reports a syntactic error.

	void match(int tokenExpected) throws SyntaxError {
		if (currentToken.kind == tokenExpected) {
			previousTokenPosition = currentToken.position;
			currentToken = scanner.getToken();
		} else {
			syntacticError("\"%\" expected here", Token.spell(tokenExpected));
		}
	}

	void accept() {
		previousTokenPosition = currentToken.position;
		currentToken = scanner.getToken();
	}

	void syntacticError(String messageTemplate, String tokenQuoted) throws SyntaxError {
		SourcePosition pos = currentToken.position;
		errorReporter.reportError(messageTemplate, tokenQuoted, pos);
		throw(new SyntaxError());
	}

	// start records the position of the start of a phrase.
	// This is defined to be the position of the first
	// character of the first token of the phrase.

	void start(SourcePosition position) {
		position.lineStart = currentToken.position.lineStart;
		position.charStart = currentToken.position.charStart;
	}

	// finish records the position of the end of a phrase.
	// This is defined to be the position of the last
	// character of the last token of the phrase.

	void finish(SourcePosition position) {
		position.lineFinish = previousTokenPosition.lineFinish;
		position.charFinish = previousTokenPosition.charFinish;
	}

	void copyStart(SourcePosition from, SourcePosition to) {
		to.lineStart = from.lineStart;
		to.charStart = from.charStart;
	}

	// ========================== PROGRAMS ========================

	public Program parseProgram() {

		Program programAST = null;

		SourcePosition programPos = new SourcePosition();
		start(programPos);

		try {
			finish(programPos);
			List dlAST = parseDeclList();
			programAST = new Program(dlAST, programPos); 
			if (currentToken.kind != Token.EOF) {
				syntacticError("\"%\" unknown type", currentToken.spelling);
			}
		}
		catch (SyntaxError s) { return null; }
		return programAST;
	}

	// ========================== DECLARATIONS ========================

	List parseDeclList() throws SyntaxError {
		SourcePosition declListPos = new SourcePosition();
		DeclList declList;
		DeclList mostChildishList;
		
		if (currentToken.kind == Token.EOF) {
			return new EmptyDeclList(dummyPos);
		} 
		declList = parseDecl();
		mostChildishList = declList;
		while(!(mostChildishList.DL instanceof EmptyDeclList)) {
			mostChildishList = ((DeclList) mostChildishList.DL);
		}
		if (currentToken.kind != Token.EOF) {
			mostChildishList.DL = parseDeclList();
		} 
		/* parse either global variable or funcion declaration until EOF */

		return declList;
	}
	

	DeclList parseDecl() throws SyntaxError {
		SourcePosition declPos = new SourcePosition();
		start(declPos);
		Type t = parseType();
		Ident i = parseIdent();
		DeclList declList;
		
		if (currentToken.kind == Token.LPAREN) {
			declList = parseFuncDeclNew(t, i, declPos);
		} else {
			declList = parseVarDeclNew(t, i, declPos);
		}
		
		return declList;
	}
	
	DeclList parseFuncDeclNew(Type t, Ident i, SourcePosition s) throws SyntaxError {
		List paraList = parseParaList();
		Stmt cmpStmt = parseCompoundStmt();
		Decl fDecl = new FuncDecl(t, i, paraList, cmpStmt, s);
		finish(s);
		List declList = new DeclList(fDecl, new EmptyDeclList(dummyPos), s);
		return ((DeclList) declList);
		
	}
	
	
	List parseVarDeclList() throws SyntaxError {
		SourcePosition declListPos = new SourcePosition();
		DeclList varDeclList;
		DeclList mostChildishDeclList;

		if (!(currentInFirst("var-decl"))) {
			return new EmptyDeclList(dummyPos);
		}
		start(declListPos);
		varDeclList = ((DeclList) parseVarDecl());
		mostChildishDeclList = varDeclList;
		/* go to the child most 'parent' */
		while (!(mostChildishDeclList.DL instanceof EmptyDeclList)) {
			mostChildishDeclList = ((DeclList) mostChildishDeclList.DL);
		}
		/* if we have more declarations then set that parent's child the tree */
		if (currentInFirst("var-decl")) {
			mostChildishDeclList.DL = parseVarDeclList();
			finish(declListPos);
			varDeclList.position = declListPos;
		}
		

		return varDeclList;
	}
	
	List parseVarDecl() throws SyntaxError {
		Type varType;
		List declaratorList;
		
		varType = parseType();
		declaratorList = parseInitDeclaratorList(varType, "local");
		match(Token.SEMICOLON);
		
		return declaratorList;
	}
	

	List parseFuncDeclList() throws SyntaxError {
		List dlAST = null;
		Decl dAST = null;

		SourcePosition funcPos = new SourcePosition();
		start(funcPos);

		dAST = parseFuncDecl();

		if (currentToken.kind == Token.VOID) {
			dlAST = parseFuncDeclList();
			finish(funcPos);
			dlAST = new DeclList(dAST, dlAST, funcPos);
		} else if (dAST != null) {
			finish(funcPos);
			dlAST = new DeclList(dAST, new EmptyDeclList(dummyPos), funcPos);
		}
		if (dlAST == null) 
			dlAST = new EmptyDeclList(dummyPos);

		return dlAST;
	}

	Decl parseFuncDecl() throws SyntaxError {

		Decl fAST = null; 

		SourcePosition funcPos = new SourcePosition();
		start(funcPos);

		Type tAST = parseType();
		Ident iAST = parseIdent();
		List fplAST = parseParaList();
		Stmt cAST = parseCompoundStmt();
		finish(funcPos);
		fAST = new FuncDecl(tAST, iAST, fplAST, cAST, funcPos);
		return fAST;
	}

	DeclList parseVarDeclNew(Type t, Ident i, SourcePosition s) throws SyntaxError {
	    DeclList declList;
		GlobalVarDecl var;
		Type varType = t;
		Expr varExp = new EmptyExpr(dummyPos);
		SourcePosition varPos = new SourcePosition();

		/* parsing declarator */
		start(varPos);
		if (currentToken.kind == Token.LBRACKET) {
			Expr arrayIntExp = new EmptyExpr(dummyPos);
			match(Token.LBRACKET);
			if (currentToken.kind == Token.INTLITERAL) {
				IntLiteral intLit = parseIntLiteral();
				arrayIntExp = new IntExpr(intLit, s);
			}
			match(Token.RBRACKET);
			finish(s);
			varType = new ArrayType(t, arrayIntExp, s);
		}
		if(currentToken.kind == Token.EQ) {
			acceptOperator();
			varExp = parseInitialiser();
		}
		finish(varPos);

		var = new GlobalVarDecl(varType, i, varExp, varPos);

		List commaDeclList = new EmptyDeclList(dummyPos);
		if (currentToken.kind == Token.COMMA) {
			match(Token.COMMA);
			/* this will return and declList*/
			commaDeclList = parseInitDeclaratorList(t, "global");

		}
		match(Token.SEMICOLON);
		
		declList = new DeclList(var, commaDeclList, varPos);
		return declList;
	}
	
	List parseInitDeclaratorList(Type varType, String declType) throws SyntaxError {
		SourcePosition declListPos = new SourcePosition();
		DeclList declList;
		Decl var;
		start(declListPos);
		var = parseInitDeclarator(varType, declType);
		if (currentToken.kind != Token.COMMA) {
			finish(declListPos);
			declList = new DeclList(var, new EmptyDeclList(dummyPos), declListPos);
		} else {
			match(Token.COMMA);
			declList = new DeclList(var, parseInitDeclaratorList(varType, declType), dummyPos);
			finish(declListPos);
			declList.position = declListPos;
		}
		
		return declList;
	}

	Decl parseInitDeclarator(Type varType, String declType) throws SyntaxError {
		Decl var;
		Expr varExp = new EmptyExpr(dummyPos); 

		var = parseDeclarator(varType, declType);
		if (currentToken.kind == Token.EQ) {
			acceptOperator();
			varExp = parseInitialiser();
		}
		if (var instanceof GlobalVarDecl) {
			((GlobalVarDecl) var).E = varExp;
		} else if (var instanceof LocalVarDecl) {
			((LocalVarDecl) var).E = varExp;
		} else {
			throw new RuntimeException("I only deal with globals and locals mate");
		}
		return var;
	}
	
	Decl parseDeclarator(Type varType, String declType) throws SyntaxError {
		SourcePosition declPos = varType.position;
		Decl var;
		Expr arrayIntExp = new EmptyExpr(dummyPos);

		Ident varIdent = parseIdent();
		if (currentToken.kind == Token.LBRACKET) {
			match(Token.LBRACKET);
			if (currentToken.kind == Token.INTLITERAL) {
				IntLiteral intLit = parseIntLiteral();
				arrayIntExp = new IntExpr(intLit, previousTokenPosition);
			}
			match(Token.RBRACKET);
			finish(declPos);
			varType = new ArrayType(varType, arrayIntExp, declPos);
		}
		finish(declPos);

		if (declType == "global") {
			var = new GlobalVarDecl(varType, varIdent, new EmptyExpr(dummyPos), declPos);
		} else if (declType == "parameter") {
			var = new ParaDecl(varType, varIdent, declPos);
		} else {
			var = new LocalVarDecl(varType, varIdent, new EmptyExpr(dummyPos), declPos);
		}

		return var;
	}
	
	Expr parseInitialiser() throws SyntaxError {
		SourcePosition initPos = new SourcePosition();
		List expList;
		Expr initialiser;

		start(initPos);
		if (currentToken.kind == Token.LCURLY) {
			match(Token.LCURLY);
			expList = parseInitialiserList();
			match(Token.RCURLY);
			finish(initPos);
			initialiser = new InitExpr(expList, initPos);
		} else {
			initialiser = parseExpr();
		}
		return initialiser;
	}
	
	List parseInitialiserList() throws SyntaxError {
		SourcePosition initListPos = new SourcePosition();
		List initList;
		Expr initExpr;
		
		start(initListPos);
		initExpr = parseExpr();
		if (currentToken.kind == Token.COMMA) {
			match(Token.COMMA);
			initList = new ExprList(initExpr, parseInitialiserList(), dummyPos);
			finish(initListPos);
			initList.position = initListPos;
		} else {
			finish(initListPos);
			initList = new ExprList(initExpr, new EmptyExprList(dummyPos), initListPos);
		}
		
		return initList;
	}
	//  ======================== TYPES ==========================

	Type parseType() throws SyntaxError {
		Type typeAST = null;

		SourcePosition typePos = new SourcePosition();
		start(typePos);

	switch(currentToken.kind) {
		case Token.VOID:
			match(Token.VOID);
			typeAST = new VoidType(dummyPos);
			break;
		case Token.BOOLEAN:
			match(Token.BOOLEAN);
			typeAST = new BooleanType(dummyPos);
			break;
		case Token.INT:
			match(Token.INT);
			typeAST = new IntType(dummyPos);
			break;
		case Token.FLOAT:
			match(Token.FLOAT);
			typeAST = new FloatType(dummyPos);
			break;
		default:
			syntacticError("expecting a type", currentToken.spelling);
		}
		finish(typePos);
		typeAST.position = typePos;
		return typeAST;
	}

	// ======================= STATEMENTS ==============================

	Stmt parseCompoundStmt() throws SyntaxError {
		SourcePosition compoundStmtPos = new SourcePosition();
		Stmt compoundStmt; 
		List varDeclList;
		List stmtList;
		
		/* if lock available, acquire it and set in funciton */
/*		if (inFunctionLock) {
			inFunctionLock = false;
			inFunction = true;
			haveLock = true;
		}
*/
		start(compoundStmtPos);
		match(Token.LCURLY);
		if (currentToken.kind == Token.RCURLY) {
			match(Token.RCURLY);
			return new EmptyCompStmt(dummyPos);
		}
		varDeclList = parseVarDeclList();
		stmtList = parseStmtList();
		match(Token.RCURLY);
		finish(compoundStmtPos);

		compoundStmt = new CompoundStmt(varDeclList, stmtList, compoundStmtPos);
		
/*		if (haveLock) {
			inFunction = false;
			inFunctionLock = true;
			haveLock = false;
		}
*/
		return compoundStmt;
	}


	List parseStmtList() throws SyntaxError {
		List slAST = null; 

		SourcePosition stmtPos = new SourcePosition();
		start(stmtPos);

		if (currentToken.kind != Token.RCURLY) {
			Stmt sAST = parseStmt();
			{
				if (currentToken.kind != Token.RCURLY) {
					slAST = parseStmtList();
					finish(stmtPos);
					slAST = new StmtList(sAST, slAST, stmtPos);
				} else {
					finish(stmtPos);
					slAST = new StmtList(sAST, new EmptyStmtList(dummyPos), stmtPos);
				}
			}
		}
		else
			slAST = new EmptyStmtList(dummyPos);

		return slAST;
	}

	Stmt parseStmt() throws SyntaxError {
		Stmt stmt = null;

		if (currentToken.kind == Token.LCURLY) {
			stmt = parseCompoundStmt();
		} else if (currentToken.kind == Token.IF) {
			stmt = parseIfStmt();
		} else if (currentToken.kind == Token.FOR) {
			stmt = parseForStmt();
		} else if (currentToken.kind == Token.WHILE) {
			stmt = parseWhileStmt();
		} else if (currentToken.kind == Token.CONTINUE) {
			stmt = parseContinueStmt();
		} else if (currentToken.kind == Token.BREAK) {
			stmt = parseBreakStmt();
		} else if (currentToken.kind == Token.RETURN) {
			stmt = parseReturnStmt();
		} else {
			stmt = parseExprStmt();
		}

		return stmt;
	}
	
	Stmt parseIfStmt() throws SyntaxError {
		SourcePosition ifPos = new SourcePosition();
		Stmt ifStmt; 
		Expr ifExpr;
		Stmt ifBody;
		Stmt elseBody;

		start(ifPos);
		match(Token.IF);
		match(Token.LPAREN);
		ifExpr = parseExpr();
		match(Token.RPAREN);
		ifBody = parseStmt();
		if (currentToken.kind == Token.ELSE) {
			match(Token.ELSE);
			elseBody = parseStmt();
			finish(ifPos);
			ifStmt = new IfStmt(ifExpr, ifBody, elseBody, ifPos);
		} else {
			finish(ifPos);
			ifStmt =  new IfStmt(ifExpr, ifBody, ifPos);
		}
		
		return ifStmt;

	}

	Stmt parseForStmt() throws SyntaxError {
		SourcePosition forPos = new SourcePosition();
		Stmt forStmt;
		Expr e1 = new EmptyExpr(dummyPos);
		Expr e2 = new EmptyExpr(dummyPos);
		Expr e3 = new EmptyExpr(dummyPos);
		Stmt forBody;
		
		start(forPos);
		match(Token.FOR);
		match(Token.LPAREN);
		if (currentToken.kind != Token.SEMICOLON) {
			e1 = parseExpr();
		}
		match(Token.SEMICOLON);
		if (currentToken.kind != Token.SEMICOLON) {
			e2 = parseExpr();
		}
		match(Token.SEMICOLON);
		if (currentToken.kind != Token.RPAREN) {
			e3 = parseExpr();
		}
		match(Token.RPAREN);
		forBody = parseStmt(); 
		finish(forPos);

		forStmt = new ForStmt(e1, e2, e3, forBody, forPos);
		
		return forStmt;
	}

	Stmt parseWhileStmt() throws SyntaxError {
		SourcePosition whilePos = new SourcePosition();
		Stmt whileStmt;
		Expr whileExpr;
		Stmt whileBody;
		
		start(whilePos);
		match(Token.WHILE);
		match(Token.LPAREN);
		whileExpr = parseExpr();
		match(Token.RPAREN);
		whileBody = parseStmt();
		finish(whilePos);
		
		whileStmt = new WhileStmt(whileExpr, whileBody, whilePos);
		
		return whileStmt;
	}

	Stmt parseBreakStmt() throws SyntaxError {
		SourcePosition breakPos = new SourcePosition();
		
		start(breakPos);
		match(Token.BREAK);
		match(Token.SEMICOLON);
		finish(breakPos);

		return new BreakStmt(breakPos);
	}

	Stmt parseContinueStmt() throws SyntaxError {
		SourcePosition continuePos = new SourcePosition();
		
		start(continuePos);
		match(Token.CONTINUE);
		match(Token.SEMICOLON);
		finish(continuePos);
		
		return new ContinueStmt(continuePos);
	}

	Stmt parseReturnStmt() throws SyntaxError {
		SourcePosition retPos = new SourcePosition();
		Expr retExpr = new EmptyExpr(dummyPos);

		start(retPos);
		match(Token.RETURN);
		if (currentToken.kind != Token.SEMICOLON) {
			retExpr = parseExpr();
		}
		match(Token.SEMICOLON);
		finish(retPos);
		
		return new ReturnStmt(retExpr, retPos);
	}
	

	Stmt parseExprStmt() throws SyntaxError {
		Stmt sAST = null;

		SourcePosition stmtPos = new SourcePosition();
		start(stmtPos);

		if (currentToken.kind != Token.SEMICOLON) {
			Expr eAST = parseExpr();
			match(Token.SEMICOLON);
			finish(stmtPos);
			sAST = new ExprStmt(eAST, stmtPos);
		} else {
			match(Token.SEMICOLON);
			finish(stmtPos);
			sAST = new ExprStmt(new EmptyExpr(dummyPos), stmtPos);
		}
		return sAST;
	}


	// ======================= PARAMETERS =======================

	List parseParaList() throws SyntaxError {
		List paraList = new EmptyParaList(dummyPos);

		SourcePosition formalsPos = new SourcePosition();
		start(formalsPos);

		match(Token.LPAREN);
		if (currentToken.kind != Token.RPAREN) {
			paraList = parseProperParaList();
		}
		match(Token.RPAREN);
		finish(formalsPos);

		return paraList;
	}

	List parseProperParaList() throws SyntaxError {
		SourcePosition paraListPos = new SourcePosition();
		List paraList;
		ParaDecl para;
		
		
		start(paraListPos);
		para = parseParaDecl();
		if (currentToken.kind == Token.COMMA) {
			match(Token.COMMA);
			paraList = new ParaList(para, parseProperParaList(), dummyPos);
			finish(paraListPos);
			paraList.position = paraListPos;
		} else {
			finish(paraListPos);
			paraList = new ParaList(para, new EmptyParaList(dummyPos), paraListPos);
		}

		return paraList;
	}

	ParaDecl parseParaDecl() throws SyntaxError {
		SourcePosition paraPos = new SourcePosition();
		ParaDecl para;
		Type paraType;

		start(paraPos);
		paraType = parseType();
		para = (ParaDecl) parseDeclarator(paraType, "parameter");
		finish(paraPos);

		return para;
	}

	List parseArgList() throws SyntaxError {
		SourcePosition argListPos = new SourcePosition();
		List argList = new EmptyArgList(dummyPos);
		
		start(argListPos);
		match(Token.LPAREN);
		if (currentToken.kind != Token.RPAREN) {
			argList = parseProperArgList();
		}
		match(Token.RPAREN);
		
		return argList;
	}

	List parseProperArgList() throws SyntaxError {
		SourcePosition argListPos = new SourcePosition();
		List argList;
		Arg arg;
		
		start(argListPos);
		arg = parseArg();
		if (currentToken.kind == Token.COMMA) {
			match(Token.COMMA);
			argList = new ArgList(arg, parseProperArgList(), dummyPos);
			finish(argListPos);
			argList.position = argListPos;
		} else {
			finish(argListPos);
			argList = new ArgList(arg, new EmptyArgList(dummyPos), argListPos);
		}
			
		return argList;
	}

	Arg parseArg() throws SyntaxError {
		SourcePosition argPos = new SourcePosition();
		Arg arg;
		
		start(argPos);
		Expr argExpr = parseExpr();
		finish(argPos);
		
		arg = new Arg(argExpr, argPos);

		return arg;
	}
	// ======================= EXPRESSIONS ======================


	Expr parseExpr() throws SyntaxError {
		Expr exprAST = null;
		exprAST = parseAssignExpr();
		return exprAST;
	}
	
	Expr parseAssignExpr() throws SyntaxError {
		SourcePosition assignExpPos = new SourcePosition();
		Expr assignExpr;
		
		start(assignExpPos);
		assignExpr = parseCondOrExpr();
		finish(assignExpPos);
		if (currentToken.kind == Token.EQ) {
			acceptOperator();
			assignExpr = new AssignExpr(assignExpr, parseAssignExpr(), dummyPos);
			finish(assignExpPos);
			assignExpr.position = assignExpPos;
		}
		
		return assignExpr;
	}
	
	
	Expr parseCondOrExpr() throws SyntaxError {
		SourcePosition condOrExprPos = new SourcePosition();
		Expr condOrExpr;
		Operator or;
		
		start(condOrExprPos);
		condOrExpr = parseCondAndExpr();
		if (currentToken.kind == Token.OROR) {
			or = acceptOperator();
			condOrExpr = new BinaryExpr(condOrExpr, or, parseCondAndExpr(), dummyPos);
			finish(condOrExprPos);
			condOrExpr.position = condOrExprPos;
		}
		
		return condOrExpr;
	}
	
	
	Expr parseCondAndExpr() throws SyntaxError {
		SourcePosition condAndExprPos = new SourcePosition();
		Expr condAndExpr;
		Operator and;
		
		start(condAndExprPos);
		condAndExpr = parseEqualityExpr();
		if (currentToken.kind == Token.ANDAND) {
			and = acceptOperator();
			condAndExpr = new BinaryExpr(condAndExpr, and, parseEqualityExpr(), dummyPos);
			finish(condAndExprPos);
			condAndExpr.position = condAndExprPos;
		}
		
		return condAndExpr;
	}
	
	
	Expr parseEqualityExpr() throws SyntaxError {
		SourcePosition equalityPos = new SourcePosition();
		Expr equalityExpr;
		Operator eqeq;
		
		start(equalityPos);
		equalityExpr = parseRelExpr();
		if (currentToken.kind == Token.EQEQ) {
			eqeq = acceptOperator();
			equalityExpr = new BinaryExpr(equalityExpr, eqeq, parseEqualityExpr(), dummyPos);
			finish(equalityPos);
			equalityExpr.position = equalityPos;
		}
		
		return equalityExpr;
	}
	
	
	Expr parseRelExpr() throws SyntaxError {
		SourcePosition relPos = new SourcePosition();
		Expr relExpr;
		Operator angleOp;
		
		start(relPos);
		relExpr = parseAdditiveExpr();
		switch(currentToken.kind) {
		case Token.LT:
		case Token.LTEQ:
		case Token.GT:
		case Token.GTEQ:
			angleOp = acceptOperator();
			relExpr = new BinaryExpr(relExpr, angleOp, parseRelExpr(), dummyPos);
			finish(relPos);
			relExpr.position = relPos;
		default:
			break;
		}
		
		return relExpr;
	}

	Expr parseAdditiveExpr() throws SyntaxError {
		Expr exprAST = null;

		SourcePosition addStartPos = new SourcePosition();
		start(addStartPos);

		exprAST = parseMultiplicativeExpr();
		while (currentToken.kind == Token.PLUS
				|| currentToken.kind == Token.MINUS) {
			Operator opAST = acceptOperator();
			Expr e2AST = parseMultiplicativeExpr();

			SourcePosition addPos = new SourcePosition();
			copyStart(addStartPos, addPos);
			finish(addPos);
			exprAST = new BinaryExpr(exprAST, opAST, e2AST, addPos);
		}
		return exprAST;
	}

	Expr parseMultiplicativeExpr() throws SyntaxError {

		Expr exprAST = null;

		SourcePosition multStartPos = new SourcePosition();
		start(multStartPos);

		exprAST = parseUnaryExpr();
		while (currentToken.kind == Token.MULT
				|| currentToken.kind == Token.DIV) {
			Operator opAST = acceptOperator();
			Expr e2AST = parseUnaryExpr();
			SourcePosition multPos = new SourcePosition();
			copyStart(multStartPos, multPos);
			finish(multPos);
			exprAST = new BinaryExpr(exprAST, opAST, e2AST, multPos);
		}
		return exprAST;
	}

	Expr parseUnaryExpr() throws SyntaxError {

		Expr exprAST = null;

		SourcePosition unaryPos = new SourcePosition();
		start(unaryPos);

		switch (currentToken.kind) {
		case Token.PLUS:
		case Token.MINUS:
		case Token.NOT:
		{
			Operator opAST = acceptOperator();
			Expr e2AST = parseUnaryExpr();
			finish(unaryPos);
			exprAST = new UnaryExpr(opAST, e2AST, unaryPos);
		}
		break;

		default:
			exprAST = parsePrimaryExpr();
			break;

		}
		return exprAST;
	}

	Expr parsePrimaryExpr() throws SyntaxError {

		Expr exprAST = null;

		SourcePosition primPos = new SourcePosition();
		start(primPos);

		switch (currentToken.kind) {

		case Token.ID:
			Ident iAST = parseIdent();
			finish(primPos);
			Var simVAST = new SimpleVar(iAST, primPos);
			exprAST = new VarExpr(simVAST, primPos);
			if (currentToken.kind == Token.LBRACKET) {
				match(Token.LBRACKET);
				Expr arrayIndexExp = parseExpr();
				match(Token.RBRACKET);
				finish(primPos);
				exprAST = new ArrayExpr(simVAST, arrayIndexExp, primPos);
			} else if (currentToken.kind == Token.LPAREN) {
				List argList = parseArgList();
				finish(primPos);
				exprAST = new CallExpr(iAST, argList, primPos);
			}
			break;
		case Token.LPAREN:
			accept();
			exprAST = parseExpr();
			match(Token.RPAREN);
			break;
		case Token.INTLITERAL:
			IntLiteral ilAST = parseIntLiteral();
			finish(primPos);
			exprAST = new IntExpr(ilAST, primPos);
			break;
		case Token.FLOATLITERAL:
			FloatLiteral floatLiteral = parseFloatLiteral();
			finish(primPos);
			exprAST = new FloatExpr(floatLiteral, primPos);
			break;
		case Token.BOOLEANLITERAL:
			BooleanLiteral boolLiteral = parseBooleanLiteral();
			finish(primPos);
			exprAST = new BooleanExpr(boolLiteral, primPos);
			break;
		case Token.STRINGLITERAL:
			StringLiteral stringLiteral = parseStringLiteral();
			finish(primPos);
			exprAST = new StringExpr(stringLiteral, primPos);
			break;
		default:
			syntacticError("illegal primary expression", currentToken.spelling);
		}

		return exprAST;
	}

	// ========================== ID, OPERATOR and LITERALS ========================

	Ident parseIdent() throws SyntaxError {

		Ident I = null; 

		if (currentToken.kind == Token.ID) {
			previousTokenPosition = currentToken.position;
			String spelling = currentToken.spelling;
			I = new Ident(spelling, previousTokenPosition);
			currentToken = scanner.getToken();
		} else 
			syntacticError("identifier expected here", "");
		return I;
	}

	// acceptOperator parses an operator, and constructs a leaf AST for it

	Operator acceptOperator() throws SyntaxError {
		Operator O = null;

		previousTokenPosition = currentToken.position;
		String spelling = currentToken.spelling;
		O = new Operator(spelling, previousTokenPosition);
		currentToken = scanner.getToken();
		return O;
	}


	IntLiteral parseIntLiteral() throws SyntaxError {
		IntLiteral IL = null;

		if (currentToken.kind == Token.INTLITERAL) {
			String spelling = currentToken.spelling;
			accept();
			IL = new IntLiteral(spelling, previousTokenPosition);
		} else 
			syntacticError("integer literal expected here", "");
		return IL;
	}

	FloatLiteral parseFloatLiteral() throws SyntaxError {
		FloatLiteral FL = null;

		if (currentToken.kind == Token.FLOATLITERAL) {
			String spelling = currentToken.spelling;
			accept();
			FL = new FloatLiteral(spelling, previousTokenPosition);
		} else 
			syntacticError("float literal expected here", "");
		return FL;
	}

	BooleanLiteral parseBooleanLiteral() throws SyntaxError {
		BooleanLiteral BL = null;

		if (currentToken.kind == Token.BOOLEANLITERAL) {
			String spelling = currentToken.spelling;
			accept();
			BL = new BooleanLiteral(spelling, previousTokenPosition);
		} else 
			syntacticError("boolean literal expected here", "");
		return BL;
	}

	StringLiteral parseStringLiteral() throws SyntaxError {
		StringLiteral strLiteral = null;

		if (currentToken.kind == Token.STRINGLITERAL) {
			String spelling = currentToken.spelling;
			accept();
			strLiteral = new StringLiteral(spelling, previousTokenPosition);
		} else {
			syntacticError("string literal expected here", "");
		}
		
		return strLiteral;
	}
}

