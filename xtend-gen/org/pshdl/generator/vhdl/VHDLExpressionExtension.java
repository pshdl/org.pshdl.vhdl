/**
 * PSHDL is a library and (trans-)compiler for PSHDL input. It generates
 *     output suitable for implementation or simulation of it.
 * 
 *     Copyright (C) 2013 Karsten Becker (feedback (at) pshdl (dot) org)
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 *     This License does not grant permission to use the trade names, trademarks,
 *     service marks, or product names of the Licensor, except as required for
 *     reasonable and customary use in describing the origin of the Work.
 * 
 * Contributors:
 *     Karsten Becker - initial API and implementation
 */
package org.pshdl.generator.vhdl;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import de.upb.hni.vmagic.AssociationElement;
import de.upb.hni.vmagic.Choices;
import de.upb.hni.vmagic.Range;
import de.upb.hni.vmagic.builtin.Standard;
import de.upb.hni.vmagic.expression.Add;
import de.upb.hni.vmagic.expression.Aggregate;
import de.upb.hni.vmagic.expression.And;
import de.upb.hni.vmagic.expression.Concatenate;
import de.upb.hni.vmagic.expression.Divide;
import de.upb.hni.vmagic.expression.Equals;
import de.upb.hni.vmagic.expression.Expression;
import de.upb.hni.vmagic.expression.FunctionCall;
import de.upb.hni.vmagic.expression.GreaterEquals;
import de.upb.hni.vmagic.expression.GreaterThan;
import de.upb.hni.vmagic.expression.LessEquals;
import de.upb.hni.vmagic.expression.LessThan;
import de.upb.hni.vmagic.expression.Literal;
import de.upb.hni.vmagic.expression.Minus;
import de.upb.hni.vmagic.expression.Multiply;
import de.upb.hni.vmagic.expression.Name;
import de.upb.hni.vmagic.expression.Not;
import de.upb.hni.vmagic.expression.NotEquals;
import de.upb.hni.vmagic.expression.Or;
import de.upb.hni.vmagic.expression.Parentheses;
import de.upb.hni.vmagic.expression.Pow;
import de.upb.hni.vmagic.expression.Rem;
import de.upb.hni.vmagic.expression.Subtract;
import de.upb.hni.vmagic.expression.Xor;
import de.upb.hni.vmagic.literal.BasedLiteral;
import de.upb.hni.vmagic.literal.CharacterLiteral;
import de.upb.hni.vmagic.literal.DecimalLiteral;
import de.upb.hni.vmagic.literal.StringLiteral;
import de.upb.hni.vmagic.object.ArrayElement;
import de.upb.hni.vmagic.object.Signal;
import de.upb.hni.vmagic.object.Slice;
import de.upb.hni.vmagic.object.Variable;
import de.upb.hni.vmagic.type.UnresolvedType;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import org.eclipse.xtend2.lib.StringConcatenation;
import org.eclipse.xtext.xbase.lib.Functions.Function2;
import org.eclipse.xtext.xbase.lib.IterableExtensions;
import org.eclipse.xtext.xbase.lib.ObjectExtensions;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure1;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure2;
import org.pshdl.generator.vhdl.VHDLFunctions;
import org.pshdl.generator.vhdl.VHDLUtils;
import org.pshdl.generator.vhdl.libraries.VHDLCastsLibrary;
import org.pshdl.generator.vhdl.libraries.VHDLShiftLibrary;
import org.pshdl.generator.vhdl.libraries.VHDLTypesLibrary;
import org.pshdl.model.HDLAnnotation;
import org.pshdl.model.HDLArithOp;
import org.pshdl.model.HDLArrayInit;
import org.pshdl.model.HDLBitOp;
import org.pshdl.model.HDLClass;
import org.pshdl.model.HDLConcat;
import org.pshdl.model.HDLEnum;
import org.pshdl.model.HDLEnumRef;
import org.pshdl.model.HDLEqualityOp;
import org.pshdl.model.HDLExpression;
import org.pshdl.model.HDLFunction;
import org.pshdl.model.HDLFunctionCall;
import org.pshdl.model.HDLInterfaceRef;
import org.pshdl.model.HDLLiteral;
import org.pshdl.model.HDLManip;
import org.pshdl.model.HDLPrimitive;
import org.pshdl.model.HDLRange;
import org.pshdl.model.HDLReference;
import org.pshdl.model.HDLShiftOp;
import org.pshdl.model.HDLTernary;
import org.pshdl.model.HDLType;
import org.pshdl.model.HDLVariable;
import org.pshdl.model.HDLVariableRef;
import org.pshdl.model.IHDLObject;
import org.pshdl.model.evaluation.HDLEvaluationContext;
import org.pshdl.model.extensions.TypeExtension;
import org.pshdl.model.types.builtIn.HDLBuiltInAnnotationProvider;
import org.pshdl.model.types.builtIn.HDLPrimitives;

@SuppressWarnings("all")
public class VHDLExpressionExtension {
  public static VHDLExpressionExtension INST = new VHDLExpressionExtension();
  
  public static Expression vhdlOf(final HDLExpression exp) {
    return VHDLExpressionExtension.INST.toVHDL(exp);
  }
  
  protected Expression _toVHDL(final HDLExpression exp) {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("Not implemented for type: ");
    HDLClass _classType = exp.getClassType();
    _builder.append(_classType);
    _builder.append(" expression is: ");
    _builder.append(exp);
    throw new IllegalArgumentException(_builder.toString());
  }
  
  protected Name _toVHDL(final HDLReference ref) {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("Not implemented for type: ");
    HDLClass _classType = ref.getClassType();
    _builder.append(_classType);
    _builder.append(" reference is: ");
    _builder.append(ref);
    throw new IllegalArgumentException(_builder.toString());
  }
  
  protected String _getVHDLName(final HDLVariableRef obj) {
    return VHDLUtils.getVHDLName(obj.getVarRefName().getLastSegment());
  }
  
  protected String _getVHDLName(final HDLInterfaceRef obj) {
    return VHDLUtils.mapName(obj);
  }
  
  protected Expression _toVHDL(final HDLVariableRef obj) {
    String _vHDLName = this.getVHDLName(obj);
    Name result = new Signal(_vHDLName, UnresolvedType.NO_NAME);
    boolean _isFrozen = obj.isFrozen();
    if (_isFrozen) {
      final Optional<HDLVariable> optHvar = obj.resolveVar();
      boolean _isPresent = optHvar.isPresent();
      if (_isPresent) {
        final HDLAnnotation memAnno = optHvar.get().getAnnotation(HDLBuiltInAnnotationProvider.HDLBuiltInAnnotations.memory);
        if ((memAnno != null)) {
          String _vHDLName_1 = VHDLUtils.getVHDLName(memAnno.getValue());
          Variable _variable = new Variable(_vHDLName_1, UnresolvedType.NO_NAME);
          result = _variable;
        }
      }
    }
    return this.getRef(result, obj);
  }
  
  private Expression getRef(final Name name, final HDLVariableRef ref) {
    Name result = name;
    int _size = ref.getArray().size();
    boolean _notEquals = (_size != 0);
    if (_notEquals) {
      final List<Expression> indices = new LinkedList<Expression>();
      ArrayList<HDLExpression> _array = ref.getArray();
      for (final HDLExpression arr : _array) {
        indices.add(this.toVHDL(arr));
      }
      ArrayElement<Name> _arrayElement = new ArrayElement<Name>(name, indices);
      result = _arrayElement;
    }
    int _size_1 = ref.getBits().size();
    boolean _greaterThan = (_size_1 > 0);
    if (_greaterThan) {
      int _size_2 = ref.getBits().size();
      boolean _greaterThan_1 = (_size_2 > 1);
      if (_greaterThan_1) {
        throw new IllegalArgumentException("Multi bit access not supported");
      }
      final HDLRange r = ref.getBits().get(0);
      HDLExpression _from = r.getFrom();
      boolean _tripleEquals = (_from == null);
      if (_tripleEquals) {
        Expression _vHDL = this.toVHDL(r.getTo());
        ArrayElement<Name> _arrayElement_1 = new ArrayElement<Name>(result, _vHDL);
        result = _arrayElement_1;
      } else {
        Range _vHDL_1 = this.toVHDL(r, Range.Direction.DOWNTO);
        Slice<Name> _slice = new Slice<Name>(result, _vHDL_1);
        result = _slice;
      }
    }
    return result;
  }
  
  protected Expression _toVHDL(final HDLArrayInit obj) {
    char _charAt = "0".charAt(0);
    CharacterLiteral _characterLiteral = new CharacterLiteral(_charAt);
    return this.toVHDLArray(obj, Aggregate.OTHERS(_characterLiteral));
  }
  
  protected Expression _toVHDLArray(final HDLExpression obj, final Expression otherValue) {
    return this.toVHDL(obj);
  }
  
  protected Expression _toVHDLArray(final HDLArrayInit obj, final Expression otherValue) {
    int _size = obj.getExp().size();
    boolean _equals = (_size == 1);
    if (_equals) {
      return this.toVHDL(obj.getExp().get(0));
    }
    final Aggregate aggr = new Aggregate();
    final Procedure2<HDLExpression, Integer> _function = (HDLExpression e, Integer i) -> {
      Expression _vHDLArray = this.toVHDLArray(e, otherValue);
      DecimalLiteral _decimalLiteral = new DecimalLiteral((i).intValue());
      aggr.createAssociation(_vHDLArray, _decimalLiteral);
    };
    IterableExtensions.<HDLExpression>forEach(obj.getExp(), _function);
    aggr.createAssociation(otherValue, Choices.OTHERS);
    return aggr;
  }
  
  protected Expression _toVHDL(final HDLInterfaceRef obj) {
    String _vHDLName = this.getVHDLName(obj);
    Name result = new Signal(_vHDLName, UnresolvedType.NO_NAME);
    int _size = obj.getIfArray().size();
    boolean _notEquals = (_size != 0);
    if (_notEquals) {
      ArrayList<HDLExpression> _ifArray = obj.getIfArray();
      LinkedList<Expression> _linkedList = new LinkedList<Expression>();
      final Function2<LinkedList<Expression>, HDLExpression, LinkedList<Expression>> _function = (LinkedList<Expression> l, HDLExpression e) -> {
        LinkedList<Expression> _xblockexpression = null;
        {
          l.add(this.toVHDL(e));
          _xblockexpression = l;
        }
        return _xblockexpression;
      };
      LinkedList<Expression> _fold = IterableExtensions.<HDLExpression, LinkedList<Expression>>fold(_ifArray, _linkedList, _function);
      ArrayElement<Name> _arrayElement = new ArrayElement<Name>(result, _fold);
      result = _arrayElement;
    }
    return this.getRef(result, obj);
  }
  
  protected Expression _toVHDL(final HDLFunctionCall obj) {
    return VHDLFunctions.toOutputExpression(obj);
  }
  
  protected Signal _toVHDL(final HDLEnumRef obj) {
    final HDLEnum hEnum = obj.resolveHEnumForced("VHDL");
    String _name = hEnum.getName();
    String _plus = ("$" + _name);
    String _plus_1 = (_plus + "_");
    String _lastSegment = obj.getVarRefName().getLastSegment();
    String _plus_2 = (_plus_1 + _lastSegment);
    String _vHDLName = VHDLUtils.getVHDLName(_plus_2);
    return new Signal(_vHDLName, UnresolvedType.NO_NAME);
  }
  
  protected Expression _toVHDL(final HDLConcat obj) {
    final List<HDLExpression> cats = obj.getCats();
    Expression res = this.toVHDL(cats.get(0));
    cats.remove(0);
    for (final HDLExpression cat : cats) {
      Expression _vHDL = this.toVHDL(cat);
      Concatenate _concatenate = new Concatenate(res, _vHDL);
      res = _concatenate;
    }
    return res;
  }
  
  protected Expression _toVHDL(final HDLManip obj) {
    HDLManip.HDLManipType _type = obj.getType();
    final HDLManip.HDLManipType type = _type;
    boolean _matched = false;
    if (Objects.equal(type, HDLManip.HDLManipType.ARITH_NEG)) {
      _matched=true;
      Expression _vHDL = this.toVHDL(obj.getTarget());
      return new Minus(_vHDL);
    }
    if (!_matched) {
      if (((type == HDLManip.HDLManipType.LOGIC_NEG) || (type == HDLManip.HDLManipType.BIT_NEG))) {
        _matched=true;
        Expression _vHDL_1 = this.toVHDL(obj.getTarget());
        return new Not(_vHDL_1);
      }
    }
    if (!_matched) {
      if (Objects.equal(type, HDLManip.HDLManipType.CAST)) {
        _matched=true;
        HDLType _castTo = obj.getCastTo();
        final HDLPrimitive targetType = ((HDLPrimitive) _castTo);
        HDLPrimitive.HDLPrimitiveType _type_1 = targetType.getType();
        boolean _tripleEquals = (_type_1 == HDLPrimitive.HDLPrimitiveType.STRING);
        if (_tripleEquals) {
          return this.toVHDL(obj.getTarget());
        }
        final HDLExpression tWidth = targetType.getWidth();
        HDLClass _classType = obj.getTarget().getClassType();
        boolean _tripleEquals_1 = (_classType == HDLClass.HDLLiteral);
        if (_tripleEquals_1) {
          HDLExpression _target = obj.getTarget();
          return VHDLCastsLibrary.handleLiteral(obj.getContainer(), ((HDLLiteral) _target), targetType, tWidth);
        }
        HDLType _typeOfForced = TypeExtension.typeOfForced(obj.getTarget(), "VHDL");
        final HDLPrimitive t = ((HDLPrimitive) _typeOfForced);
        Expression exp = this.toVHDL(obj.getTarget());
        HDLPrimitive.HDLPrimitiveType actualType = t.getType();
        if ((tWidth != null)) {
          final VHDLCastsLibrary.TargetType resized = VHDLCastsLibrary.getResize(exp, t, tWidth);
          exp = resized.resized;
          actualType = resized.newType;
        }
        HDLType _meta = obj.<HDLType>getMeta(HDLManip.WRONG_TYPE);
        final HDLPrimitive meta = ((HDLPrimitive) _meta);
        if ((meta != null)) {
          actualType = meta.getType();
        }
        return VHDLCastsLibrary.cast(exp, actualType, targetType.getType());
      }
    }
    throw new IllegalArgumentException(("Not supported:" + obj));
  }
  
  public Range toVHDL(final HDLRange obj, final Range.Direction dir) {
    HDLEvaluationContext _hDLEvaluationContext = new HDLEvaluationContext();
    final Procedure1<HDLEvaluationContext> _function = (HDLEvaluationContext it) -> {
      it.ignoreConstantRefs = true;
      it.ignoreParameterRefs = true;
    };
    final HDLEvaluationContext context = ObjectExtensions.<HDLEvaluationContext>operator_doubleArrow(_hDLEvaluationContext, _function);
    final Expression to = this.toVHDL(HDLPrimitives.simplifyWidth(obj, obj.getTo(), context));
    HDLExpression _from = obj.getFrom();
    boolean _tripleEquals = (_from == null);
    if (_tripleEquals) {
      return new Range(to, dir, to);
    }
    Expression _vHDL = this.toVHDL(HDLPrimitives.simplifyWidth(obj, obj.getFrom(), context));
    return new Range(_vHDL, dir, to);
  }
  
  protected Literal _toVHDL(final HDLLiteral obj) {
    int length = (-1);
    BigInteger _valueAsBigInt = obj.getValueAsBigInt();
    boolean _tripleNotEquals = (_valueAsBigInt != null);
    if (_tripleNotEquals) {
      length = obj.getValueAsBigInt().bitLength();
    }
    return this.toVHDL(obj, length, false);
  }
  
  public Literal toVHDL(final HDLLiteral obj, final int length, final boolean asString) {
    int l = length;
    String sVal = obj.getVal();
    if ((l == 0)) {
      l = 1;
    }
    final BigInteger dec = obj.getValueAsBigInt();
    HDLLiteral.HDLLiteralPresentation _presentation = obj.getPresentation();
    if (_presentation != null) {
      switch (_presentation) {
        case STR:
          return new StringLiteral(sVal);
        case BOOL:
          boolean _equals = "true".equals(sVal);
          if (_equals) {
            return Standard.BOOLEAN_TRUE;
          }
          return Standard.BOOLEAN_FALSE;
        case HEX:
          if ((asString || (dec.bitLength() > 32))) {
            return VHDLUtils.toHexLiteral(l, dec);
          }
          StringConcatenation _builder = new StringConcatenation();
          _builder.append("16#");
          String _substring = sVal.substring(2);
          _builder.append(_substring);
          _builder.append("#");
          return new BasedLiteral(_builder.toString());
        case BIN:
          if ((asString || (dec.bitLength() > 32))) {
            return VHDLUtils.toBinaryLiteral(l, dec);
          }
          StringConcatenation _builder_1 = new StringConcatenation();
          _builder_1.append("2#");
          String _substring_1 = sVal.substring(2);
          _builder_1.append(_substring_1);
          _builder_1.append("#");
          return new BasedLiteral(_builder_1.toString());
        default:
          break;
      }
    }
    if (((dec.bitLength() > 31) || asString)) {
      return VHDLUtils.toBinaryLiteral(l, dec);
    }
    return new DecimalLiteral(sVal);
  }
  
  protected Expression _toVHDL(final HDLShiftOp obj) {
    HDLType _typeOfForced = TypeExtension.typeOfForced(obj.getLeft(), "VHDL");
    final HDLPrimitive type = ((HDLPrimitive) _typeOfForced);
    return VHDLShiftLibrary.shift(this.toVHDL(obj.getLeft()), this.toVHDL(obj.getRight()), type.getType(), obj.getType());
  }
  
  protected Expression _toVHDL(final HDLEqualityOp obj) {
    HDLEqualityOp.HDLEqualityOpType _type = obj.getType();
    if (_type != null) {
      switch (_type) {
        case EQ:
          Expression _vHDL = this.toVHDL(obj.getLeft());
          Expression _vHDL_1 = this.toVHDL(obj.getRight());
          Equals _equals = new Equals(_vHDL, _vHDL_1);
          return new Parentheses(_equals);
        case GREATER_EQ:
          Expression _vHDL_2 = this.toVHDL(obj.getLeft());
          Expression _vHDL_3 = this.toVHDL(obj.getRight());
          GreaterEquals _greaterEquals = new GreaterEquals(_vHDL_2, _vHDL_3);
          return new Parentheses(_greaterEquals);
        case GREATER:
          Expression _vHDL_4 = this.toVHDL(obj.getLeft());
          Expression _vHDL_5 = this.toVHDL(obj.getRight());
          GreaterThan _greaterThan = new GreaterThan(_vHDL_4, _vHDL_5);
          return new Parentheses(_greaterThan);
        case LESS_EQ:
          Expression _vHDL_6 = this.toVHDL(obj.getLeft());
          Expression _vHDL_7 = this.toVHDL(obj.getRight());
          LessEquals _lessEquals = new LessEquals(_vHDL_6, _vHDL_7);
          return new Parentheses(_lessEquals);
        case LESS:
          Expression _vHDL_8 = this.toVHDL(obj.getLeft());
          Expression _vHDL_9 = this.toVHDL(obj.getRight());
          LessThan _lessThan = new LessThan(_vHDL_8, _vHDL_9);
          return new Parentheses(_lessThan);
        case NOT_EQ:
          Expression _vHDL_10 = this.toVHDL(obj.getLeft());
          Expression _vHDL_11 = this.toVHDL(obj.getRight());
          NotEquals _notEquals = new NotEquals(_vHDL_10, _vHDL_11);
          return new Parentheses(_notEquals);
        default:
          break;
      }
    }
    throw new IllegalArgumentException(("Not supported:" + obj));
  }
  
  protected Expression _toVHDL(final HDLBitOp obj) {
    HDLBitOp.HDLBitOpType _type = obj.getType();
    final HDLBitOp.HDLBitOpType type = _type;
    boolean _matched = false;
    if (((type == HDLBitOp.HDLBitOpType.AND) || (type == HDLBitOp.HDLBitOpType.LOGI_AND))) {
      _matched=true;
      Expression _vHDL = this.toVHDL(obj.getLeft());
      Expression _vHDL_1 = this.toVHDL(obj.getRight());
      And _and = new And(_vHDL, _vHDL_1);
      return new Parentheses(_and);
    }
    if (!_matched) {
      if (((type == HDLBitOp.HDLBitOpType.OR) || (type == HDLBitOp.HDLBitOpType.LOGI_OR))) {
        _matched=true;
        Expression _vHDL_2 = this.toVHDL(obj.getLeft());
        Expression _vHDL_3 = this.toVHDL(obj.getRight());
        Or _or = new Or(_vHDL_2, _vHDL_3);
        return new Parentheses(_or);
      }
    }
    if (!_matched) {
      if (Objects.equal(type, HDLBitOp.HDLBitOpType.XOR)) {
        _matched=true;
        Expression _vHDL_4 = this.toVHDL(obj.getLeft());
        Expression _vHDL_5 = this.toVHDL(obj.getRight());
        Xor _xor = new Xor(_vHDL_4, _vHDL_5);
        return new Parentheses(_xor);
      }
    }
    throw new IllegalArgumentException(("Not supported:" + obj));
  }
  
  protected Expression _toVHDL(final HDLArithOp obj) {
    HDLArithOp.HDLArithOpType _type = obj.getType();
    if (_type != null) {
      switch (_type) {
        case PLUS:
          Expression _vHDL = this.toVHDL(obj.getLeft());
          Expression _vHDL_1 = this.toVHDL(obj.getRight());
          Add _add = new Add(_vHDL, _vHDL_1);
          return new Parentheses(_add);
        case MINUS:
          Expression _vHDL_2 = this.toVHDL(obj.getLeft());
          Expression _vHDL_3 = this.toVHDL(obj.getRight());
          Subtract _subtract = new Subtract(_vHDL_2, _vHDL_3);
          return new Parentheses(_subtract);
        case DIV:
          Expression _vHDL_4 = this.toVHDL(obj.getLeft());
          Expression _vHDL_5 = this.toVHDL(obj.getRight());
          Divide _divide = new Divide(_vHDL_4, _vHDL_5);
          return new Parentheses(_divide);
        case MUL:
          Expression _vHDL_6 = this.toVHDL(obj.getLeft());
          Expression _vHDL_7 = this.toVHDL(obj.getRight());
          Multiply _multiply = new Multiply(_vHDL_6, _vHDL_7);
          return new Parentheses(_multiply);
        case MOD:
          Expression _vHDL_8 = this.toVHDL(obj.getLeft());
          Expression _vHDL_9 = this.toVHDL(obj.getRight());
          Rem _rem = new Rem(_vHDL_8, _vHDL_9);
          return new Parentheses(_rem);
        case POW:
          Expression _vHDL_10 = this.toVHDL(obj.getLeft());
          Expression _vHDL_11 = this.toVHDL(obj.getRight());
          Pow _pow = new Pow(_vHDL_10, _vHDL_11);
          return new Parentheses(_pow);
        default:
          break;
      }
    }
    throw new IllegalArgumentException(("Not supported:" + obj));
  }
  
  protected Expression _toVHDL(final HDLTernary obj) {
    final FunctionCall fc = new FunctionCall(VHDLTypesLibrary.TERNARY_SLV);
    final List<AssociationElement> parameters = fc.getParameters();
    Expression _vHDL = this.toVHDL(obj.getIfExpr());
    AssociationElement _associationElement = new AssociationElement(_vHDL);
    parameters.add(_associationElement);
    Expression _vHDL_1 = this.toVHDL(obj.getThenExpr());
    AssociationElement _associationElement_1 = new AssociationElement(_vHDL_1);
    parameters.add(_associationElement_1);
    Expression _vHDL_2 = this.toVHDL(obj.getElseExpr());
    AssociationElement _associationElement_2 = new AssociationElement(_vHDL_2);
    parameters.add(_associationElement_2);
    return fc;
  }
  
  protected Expression _toVHDL(final HDLFunction obj) {
    throw new IllegalArgumentException(("Not supported:" + obj));
  }
  
  public Expression toVHDL(final IHDLObject obj) {
    if (obj instanceof HDLInterfaceRef) {
      return _toVHDL((HDLInterfaceRef)obj);
    } else if (obj instanceof HDLEnumRef) {
      return _toVHDL((HDLEnumRef)obj);
    } else if (obj instanceof HDLVariableRef) {
      return _toVHDL((HDLVariableRef)obj);
    } else if (obj instanceof HDLArithOp) {
      return _toVHDL((HDLArithOp)obj);
    } else if (obj instanceof HDLBitOp) {
      return _toVHDL((HDLBitOp)obj);
    } else if (obj instanceof HDLEqualityOp) {
      return _toVHDL((HDLEqualityOp)obj);
    } else if (obj instanceof HDLFunction) {
      return _toVHDL((HDLFunction)obj);
    } else if (obj instanceof HDLShiftOp) {
      return _toVHDL((HDLShiftOp)obj);
    } else if (obj instanceof HDLArrayInit) {
      return _toVHDL((HDLArrayInit)obj);
    } else if (obj instanceof HDLConcat) {
      return _toVHDL((HDLConcat)obj);
    } else if (obj instanceof HDLFunctionCall) {
      return _toVHDL((HDLFunctionCall)obj);
    } else if (obj instanceof HDLLiteral) {
      return _toVHDL((HDLLiteral)obj);
    } else if (obj instanceof HDLManip) {
      return _toVHDL((HDLManip)obj);
    } else if (obj instanceof HDLReference) {
      return _toVHDL((HDLReference)obj);
    } else if (obj instanceof HDLTernary) {
      return _toVHDL((HDLTernary)obj);
    } else if (obj instanceof HDLExpression) {
      return _toVHDL((HDLExpression)obj);
    } else {
      throw new IllegalArgumentException("Unhandled parameter types: " +
        Arrays.<Object>asList(obj).toString());
    }
  }
  
  public String getVHDLName(final HDLVariableRef obj) {
    if (obj instanceof HDLInterfaceRef) {
      return _getVHDLName((HDLInterfaceRef)obj);
    } else if (obj != null) {
      return _getVHDLName(obj);
    } else {
      throw new IllegalArgumentException("Unhandled parameter types: " +
        Arrays.<Object>asList(obj).toString());
    }
  }
  
  public Expression toVHDLArray(final HDLExpression obj, final Expression otherValue) {
    if (obj instanceof HDLArrayInit) {
      return _toVHDLArray((HDLArrayInit)obj, otherValue);
    } else if (obj != null) {
      return _toVHDLArray(obj, otherValue);
    } else {
      throw new IllegalArgumentException("Unhandled parameter types: " +
        Arrays.<Object>asList(obj, otherValue).toString());
    }
  }
}
