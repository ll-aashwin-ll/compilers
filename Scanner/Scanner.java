/**
 * *	Scanner.java
 **/

package VC.Scanner;

import VC.ErrorReporter;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

public final class Scanner { 

	private SourceFile sourceFile;
	private boolean debug;

	private ErrorReporter errorReporter;
	private StringBuffer currentSpelling;
	private char currentChar;
	private SourcePosition sourcePos;
	private List<Character> escapeChars = new ArrayList<Character>(Arrays.asList('b', 'f', 'n', 'r', 't', '\'', '"', '\\'));

	// =========================================================

	public Scanner(SourceFile source, ErrorReporter reporter) {
		sourceFile = source;
		errorReporter = reporter;
		currentChar = sourceFile.getNextChar();
		debug = false;

		// you may initialise your counters for line and column numbers here
	}

	public void enableDebugging() {
		debug = true;
	}

	// accept gets the next character from the source program.

	private void accept() {

		currentChar = sourceFile.getNextChar();
		if (currentChar == '\n') {
			System.out.println("the current char is the linefeed character");
		} else if (currentChar == '\r') {
			System.out.println("the current char is the carridge return character");
		} else {
			System.out.println("accept(): the curr char is now " + currentChar);
		}

		// you may save the lexeme of the current token incrementally here
		// you may also increment your line and column counters here
	}

	// inspectChar returns the n-th character after currentChar
	// in the input stream.
	//
	// If there are fewer than nthChar characters between currentChar
	// and the end of file marker, SourceFile.eof is returned.
	//
	// Both currentChar and the current position in the input stream
	// are *not* changed. Therefore, a subsequent call to accept()
	// will always return the next char after currentChar.

	private char inspectChar(int nthChar) {
		return sourceFile.inspectChar(nthChar);
	}

	private int checkSeperators() {
		int retVal = -1;
		System.out.println("checkSeperators(): entered");
		switch (currentChar) {
			case '(':
				accept();
				retVal = Token.LPAREN;
				break;
			case ')':
				accept();
				retVal = Token.RPAREN;
				break;
			case '{':
				accept();
				retVal = Token.LCURLY;
				break;
			case '}':
				accept();
				retVal = Token.RCURLY;
				break;
			case '[':
				accept();
				retVal = Token.LBRACKET;
				break;
			case ']':
				accept();
				retVal = Token.RBRACKET;
				break;
			case ';':
				accept();
				retVal = Token.SEMICOLON;
				break;
			case ',':
				accept();
				retVal = Token.COMMA;
				break;
			default:
				System.out.println("checkSeperators: recieved invalid token type");
		}
		if (retVal > 0) {
			currentSpelling.append(Token.spell(retVal));
		}
		return retVal;
	}

	private int checkOperators() {
		int retVal = -1;
		// TODO: find a way to add spelling 
		System.out.println("checkOperators(): entered");
		switch(currentChar) { 
			case '|':
				if (inspectChar(1) == '|') {
					// accept twice cos two characters
					accept();
					accept();
					retVal = Token.OROR;
				}
				break;
			case '+':
				accept();
				retVal = Token.PLUS;
				break;
			case '-':
				accept();
				retVal = Token.MINUS;
				break;
			case '*':
				accept();
				retVal = Token.MULT;
				break;
			case '/':
				accept();
				retVal = Token.DIV;
				break;
			case '!':
				if (inspectChar(1) == '=') {
					accept(); accept();
					retVal = Token.NOTEQ;
				}
				else {
					retVal = Token.NOT;
                    accept();
				}
				break;
			case '=':
				if (currentChar == '=') {
					accept();
					retVal = Token.EQEQ;
				} else {
					retVal = Token.EQ;
                    accept();
				}
				break;
			case '<':
				if (currentChar == '=') {
					accept();
					retVal = Token.LTEQ;
				} else {
					retVal = Token.LT;
                    accept();
				}
				break;
			case '>':
				if (currentChar == '=') {
					accept();
					retVal = Token.GTEQ;
				} else {
					retVal = Token.GT;
                    accept();
				}
				break;
			case '&':
				if (inspectChar(1) == '&') {
					accept(); accept();
					retVal = Token.ANDAND;
				}
				accept();
				break;
			default:
				System.out.println("checkOperators: received invalid token type");
		}
		if (retVal > 0) {
			currentSpelling.append(Token.spell(retVal));
		}
		return retVal;
	}

	private int checkIdentifiers() {
		System.out.println("checkIdentifiers(): entered");
		// check for IDs
		// has to start with letter or underscore
		if (currentChar >= 'a' && currentChar <= 'z'
				|| currentChar >= 'A' && currentChar <= 'Z'
				|| currentChar == '_') {
            currentSpelling.append(currentChar);
			accept();
			while (true) {
				// now numbers in ID are valid
				if (currentChar >= 'a' && currentChar <= 'z'
						|| currentChar >= 'A' && currentChar <= 'Z'
						|| currentChar >= '0' && currentChar <= '9'
						|| currentChar == '_') {
                    currentSpelling.append(currentChar);
					accept();
					// the identifier spells a boolean value, 
					if (currentSpelling.toString().equals("true") ||
							currentSpelling.toString().equals("false")) {
						return Token.ID;
							}
				} else {
					// we didn't find a valid char for ID, return
					return Token.ID;
				}
			}
		} else {
			System.out.println("checkIdentifers: received invalid token type");
			return -1;
		}
	}

	private char getEscapeChar(char ch) {
		System.out.println("getEscapeChar(): entered");
		char retVal = Character.MIN_VALUE;
		if (ch == 'b') {
			retVal = '\b';
		} else if (ch == 'r') {
			retVal = '\r';
		} else if (ch == 'n') {
			retVal = '\n';
		} else if (ch == 't') {
			retVal = '\t';
		} else if (ch == 'f') {
			retVal = '\f';
		} else if (ch == '\'') {
			retVal = '\'';
		} else if (ch == '"') {
			retVal = '"';
		} else if (ch == '\\') {
			retVal = '\\';
		} else {
			System.out.println("no escape char found");
		}
		return retVal;
	}

	private int checkLiterals() {
		//TODO: boolean literals
		System.out.println("checkLiterals(): entered");
		// TODO: this is checking for ID's, it should be checking for string literials 
		boolean isFloat = false;
		boolean isInt = false;
		int retVal = -1;
		if (currentChar == '"' ) {
			// add to spelling until we find the terminting " keeping eating the chars
			// TODO: handle escaping of quotation marks
			do {
				accept();
				// TODO: check for CRLF as well, maybe you might have to accept twice
				if (currentChar == '\n' || currentChar == '\r') {
					errorReporter.reportError("unterminated string", currentSpelling.toString(), sourcePos);
					return Token.STRINGLITERAL;
				} else if (currentChar == '\\') {
					if (escapeChars.contains(inspectChar(1))) {
						System.out.println("checkLiterals(): got an escape character");
						currentSpelling.append(getEscapeChar(inspectChar(1)));
						accept();
						continue;
					} else {
						String illegal_escape = new StringBuilder().append("").append(currentChar).append(inspectChar(1)).toString();
						System.out.println("checkLiterals(): the illegal escape character is " + illegal_escape);
						errorReporter.reportError("%: illegal escape character", illegal_escape, sourcePos);
						currentSpelling.append(currentChar);
					}
				} else {
					System.out.println("checkLiterals(): just a normal char, adding it to spelling");
					currentSpelling.append(currentChar);
				}

			} while (currentChar != '"');
			// get rid of the last quotation
			currentSpelling.deleteCharAt(currentSpelling.length()-1);
			// accept once for the extra quotation
			accept();
			System.out.println("checkLiterals: we've hit the end of a string and now returning");
			return Token.STRINGLITERAL;
		} else if (currentChar >= '0' && currentChar <= '9') {
			retVal = Token.INTLITERAL;
			currentSpelling.append(currentChar);
			accept();
			while (currentChar >= '0' && currentChar <= '9') {
				currentSpelling.append(currentChar);
				accept();
			}
		}
		if (currentChar == '.') {
			if (inspectChar(1) >= '0' && inspectChar(1) <= '9') {
				currentSpelling.append(currentChar);
				// look ahead one char, if number accept and add to spelling
				while (inspectChar(1) >= '0' && inspectChar(1) <= '9') {
					accept();
					currentSpelling.append(currentChar);
					retVal = Token.FLOATLITERAL;
				}
				// accept, otherwise we'll get duplicate reading
				accept();
			}
		}
		if (currentChar == 'e' || currentChar == 'E') {
			if (inspectChar(1) >= '0' && inspectChar(1) <= '9') {
				currentSpelling.append(currentChar);
				// look ahead one char, if number accept and add to spelling
				while (inspectChar(1) >= '0' && inspectChar(1) <= '9') {
					accept();
					currentSpelling.append(currentChar);
					retVal = Token.FLOATLITERAL;
				}
				// accept, otherwise we'll get duplicate reading
				accept();
			}
		}

		if (retVal < 0) {
			System.out.println("checkLiterals: received invalid token type");
		}
		return retVal;
			// first check for ints adding to the spelling, retVal=INT
			// if dot or 'e/E' and digits after, valid add to spelling, retVal=FLOAT
			// we can make them independnet if's, 
	}

	// TODO: this should return an int
	private int checkSpecial() {
		System.out.println("checkSpecial(): entered");
		if (currentChar == SourceFile.eof) {
			currentSpelling.append(Token.spell(Token.EOF));
			return Token.EOF;
		} else {
			System.out.println("checkSpecial: received invalid token type");
			return -1;
		}

	}

	private enum Tokens {
		SEPERATORS, OPERATORS, LITERALS,
		IDENTIFIERS, KEYWORDS, SPECIAL
	}

	private int tokenChecker(Tokens tokenType) {
		switch (tokenType) {
			case SEPERATORS:
				return checkSeperators();
			case OPERATORS: 
				return checkOperators();
			case LITERALS:
				return checkLiterals();
			case IDENTIFIERS:
				return checkIdentifiers();
			case KEYWORDS:
				return checkIdentifiers();
			case SPECIAL:
				return checkSpecial();
			default:
				System.out.println("tokenChecker: invalid token type");
				return -1;
		}
	}

	private int nextToken() {
		// Tokens: separators, operators, literals, identifiers and keyworods
		int tokenID = -1;
		for (Tokens tokenType: Tokens.values()) {
			tokenID = tokenChecker(tokenType);
			if (tokenID >= 0) {
				return tokenID;
			}
		}

		System.out.println("nextToken: error in identifying token");
        // erroneous token spelling
        if (currentSpelling.toString().equals("")) {
            currentSpelling.append(currentChar);
        }
		// to flush the errenous curr char out
		accept();
		return Token.ERROR;
		// TODO: remove this
		/*case SourceFile.eof:
		  =======

		  case SourceFile.eof:
		  >>>>>>> 72ae9d7913625bf0971709a0b36ac8ed30be3374
		  currentSpelling.append(Token.spell(Token.EOF));
		  return Token.EOF;
		  default:
		  break;*/
	}

	void skipSpaceAndComments() {
		// TODO: handle multiline comments
		System.out.println("skipSpaceAndComments(): entered");
		// skipping whitespace
		if (Character.isWhitespace(currentChar)) {
			accept();
			skipSpaceAndComments();
		}
		// skipping comments
		if (currentChar == '/') {
			if (inspectChar(1) == '/') {
				System.out.println("skipSpaceAndComents: comment detected"); 
				// if we find '//' remove all chars until we hit LF or CR
				while (currentChar != '\n' && currentChar != '\r') {
					accept();
				}
                // we've hit the new line, now remove the newline
				if (currentChar == '\r' && inspectChar(1) == '\n') {
					// if CRLF then remove both
					accept();
					accept();
				} else {
					// if either only CR or LF remove one char
					accept();
				}
				skipSpaceAndComments();
			} else if (inspectChar(1) == '*') {
                // multiline comment
                System.out.println("skipSpaceAndComments(): start of multiline string");
                accept(); accept();
                while (true) {
                    if (currentChar == '*' && inspectChar(1) == '/') {
                        System.out.println("skipSpaceAndComments(); end of multilin string");
                        accept(); accept();
                        skipSpaceAndComments();
                        break;
                    } else if (currentChar == SourceFile.eof) {
                        errorReporter.reportError("Unterminated comment", "/**/", sourcePos);
                        break;
                    }
                    accept();
                }
            }
		} else if (currentChar == '\n' || currentChar == '\r') {
			System.out.println("skipSpaceAndComents: there line terminator detected");
			// if the current token is a line terminator remove it
			if (currentChar == '\r' && inspectChar(1) == '\n') {
				System.out.println("skipSpaceAndComents: CRLF detected");
				// if CRLF then remove both
				accept();
				accept();
			} else {
				System.out.println("skipSpaceAndComents: either CR or LF detected");
				// if either only CR or LF remove one char
				// TODO: I have no idea why I need two accepts here
				accept();
			}
			skipSpaceAndComments();
		} 
		// removing line terminators
	}

	public Token getToken() {
		Token tok;
		int kind;

		System.out.println("getToken: getting new token");

		currentSpelling = new StringBuffer("");

		sourcePos = new SourcePosition();
        
	    errorReporter = new ErrorReporter();

		// skip white space and comments
		skipSpaceAndComments();

		// You must record the position of the current token somehow

		kind = nextToken();

		// You need three types of information to contstruct a token objects
		// its kings represented as an 'int' (Token.ID, Token.PLUS)
		// its pselling represented as a string ("sum", "+")
		// its position in the program represented as an object of the lcass SourcePosition
		tok = new Token(kind, currentSpelling.toString(), sourcePos);

		// * do not remove these three lines
		if (debug)
			System.out.println(tok);
		return tok;
	}

}

// QUESTION:
// To what extent do we remove all whitespace?
//      if we have the int 33 and the float 4.4
//      and we remove white space to become 334.4
//      how do we now recognize these as two tokens.
