package de.fub.bytecode.generic;

/** 
 * LXOR - Bitwise XOR long
 * <PRE>Stack: ..., value1, value2 -&gt; ..., result</PRE>
 *
 * @version $Id: LXOR.java,v 1.5 2001/05/09 09:26:57 dahm Exp $
 * @authXOR  <A HREF="http://www.berlin.de/~markus.dahm/">M. Dahm</A>
 */
public class LXOR extends ArithmeticInstruction {
  public LXOR() {
    super(de.fub.bytecode.Constants.LXOR);
  }


  /**
   * Call corresponding visitor method(s). The order is:
   * Call visitor methods of implemented interfaces first, then
   * call methods according to the class hierarchy in descending order,
   * i.e., the most specific visitXXX() call comes last.
   *
   * @param v Visitor object
   */
  public void accept(Visitor v) {
    v.visitTypedInstruction(this);
    v.visitStackProducer(this);
    v.visitStackConsumer(this);
    v.visitArithmeticInstruction(this);
    v.visitLXOR(this);
  }
}
