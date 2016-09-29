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
import org.pshdl.model.utils.HDLQualifiedName;

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

@SuppressWarnings("all")
public class VHDLExpressionExtension {
	public static VHDLExpressionExtension INST = new VHDLExpressionExtension();

	public static Expression vhdlOf(final HDLExpression exp) {
		return VHDLExpressionExtension.INST.toVHDL(exp);
	}

	protected Expression _toVHDL(final HDLExpression exp) {
		final StringConcatenation _builder = new StringConcatenation();
		_builder.append("Not implemented for type: ");
		final HDLClass _classType = exp.getClassType();
		_builder.append(_classType, "");
		_builder.append(" expression is: ");
		_builder.append(exp, "");
		throw new IllegalArgumentException(_builder.toString());
	}

	protected Name _toVHDL(final HDLReference ref) {
		final StringConcatenation _builder = new StringConcatenation();
		_builder.append("Not implemented for type: ");
		final HDLClass _classType = ref.getClassType();
		_builder.append(_classType, "");
		_builder.append(" reference is: ");
		_builder.append(ref, "");
		throw new IllegalArgumentException(_builder.toString());
	}

	protected String _getVHDLName(final HDLVariableRef obj) {
		final HDLQualifiedName _varRefName = obj.getVarRefName();
		final String _lastSegment = _varRefName.getLastSegment();
		return VHDLUtils.getVHDLName(_lastSegment);
	}

	protected String _getVHDLName(final HDLInterfaceRef obj) {
		return VHDLUtils.mapName(obj);
	}

	protected Expression _toVHDL(final HDLVariableRef obj) {
		final String _vHDLName = this.getVHDLName(obj);
		Name result = new Signal(_vHDLName, UnresolvedType.NO_NAME);
		final boolean _isFrozen = obj.isFrozen();
		if (_isFrozen) {
			final Optional<HDLVariable> optHvar = obj.resolveVar();
			final boolean _isPresent = optHvar.isPresent();
			if (_isPresent) {
				final HDLVariable _get = optHvar.get();
				final HDLAnnotation memAnno = _get.getAnnotation(HDLBuiltInAnnotationProvider.HDLBuiltInAnnotations.memory);
				if ((memAnno != null)) {
					final String _value = memAnno.getValue();
					final String _vHDLName_1 = VHDLUtils.getVHDLName(_value);
					final Variable _variable = new Variable(_vHDLName_1, UnresolvedType.NO_NAME);
					result = _variable;
				}
			}
		}
		return this.getRef(result, obj);
	}

	private Expression getRef(final Name name, final HDLVariableRef ref) {
		Name result = name;
		final ArrayList<HDLExpression> _array = ref.getArray();
		final int _size = _array.size();
		final boolean _notEquals = (_size != 0);
		if (_notEquals) {
			final List<Expression> indices = new LinkedList<Expression>();
			final ArrayList<HDLExpression> _array_1 = ref.getArray();
			for (final HDLExpression arr : _array_1) {
				final Expression _vHDL = this.toVHDL(arr);
				indices.add(_vHDL);
			}
			final ArrayElement<Name> _arrayElement = new ArrayElement<Name>(name, indices);
			result = _arrayElement;
		}
		final ArrayList<HDLRange> _bits = ref.getBits();
		final int _size_1 = _bits.size();
		final boolean _greaterThan = (_size_1 > 0);
		if (_greaterThan) {
			final ArrayList<HDLRange> _bits_1 = ref.getBits();
			final int _size_2 = _bits_1.size();
			final boolean _greaterThan_1 = (_size_2 > 1);
			if (_greaterThan_1)
				throw new IllegalArgumentException("Multi bit access not supported");
			final ArrayList<HDLRange> _bits_2 = ref.getBits();
			final HDLRange r = _bits_2.get(0);
			final HDLExpression _from = r.getFrom();
			final boolean _tripleEquals = (_from == null);
			if (_tripleEquals) {
				final HDLExpression _to = r.getTo();
				final Expression _vHDL_1 = this.toVHDL(_to);
				final ArrayElement<Name> _arrayElement_1 = new ArrayElement<Name>(result, _vHDL_1);
				result = _arrayElement_1;
			} else {
				final Range _vHDL_2 = this.toVHDL(r, Range.Direction.DOWNTO);
				final Slice<Name> _slice = new Slice<Name>(result, _vHDL_2);
				result = _slice;
			}
		}
		return result;
	}

	protected Expression _toVHDL(final HDLArrayInit obj) {
		final char _charAt = "0".charAt(0);
		final CharacterLiteral _characterLiteral = new CharacterLiteral(_charAt);
		final Aggregate _OTHERS = Aggregate.OTHERS(_characterLiteral);
		return this.toVHDLArray(obj, _OTHERS);
	}

	protected Expression _toVHDLArray(final HDLExpression obj, final Expression otherValue) {
		return this.toVHDL(obj);
	}

	protected Expression _toVHDLArray(final HDLArrayInit obj, final Expression otherValue) {
		final ArrayList<HDLExpression> _exp = obj.getExp();
		final int _size = _exp.size();
		final boolean _equals = (_size == 1);
		if (_equals) {
			final ArrayList<HDLExpression> _exp_1 = obj.getExp();
			final HDLExpression _get = _exp_1.get(0);
			return this.toVHDL(_get);
		}
		final Aggregate aggr = new Aggregate();
		final ArrayList<HDLExpression> _exp_2 = obj.getExp();
		final Procedure2<HDLExpression, Integer> _function = new Procedure2<HDLExpression, Integer>() {
			@Override
			public void apply(final HDLExpression e, final Integer i) {
				final Expression _vHDLArray = VHDLExpressionExtension.this.toVHDLArray(e, otherValue);
				final DecimalLiteral _decimalLiteral = new DecimalLiteral((i).intValue());
				aggr.createAssociation(_vHDLArray, _decimalLiteral);
			}
		};
		IterableExtensions.<HDLExpression> forEach(_exp_2, _function);
		aggr.createAssociation(otherValue, Choices.OTHERS);
		return aggr;
	}

	protected Expression _toVHDL(final HDLInterfaceRef obj) {
		final String _vHDLName = this.getVHDLName(obj);
		Name result = new Signal(_vHDLName, UnresolvedType.NO_NAME);
		final ArrayList<HDLExpression> _ifArray = obj.getIfArray();
		final int _size = _ifArray.size();
		final boolean _notEquals = (_size != 0);
		if (_notEquals) {
			final ArrayList<HDLExpression> _ifArray_1 = obj.getIfArray();
			final LinkedList<Expression> _linkedList = new LinkedList<Expression>();
			final Function2<LinkedList<Expression>, HDLExpression, LinkedList<Expression>> _function = new Function2<LinkedList<Expression>, HDLExpression, LinkedList<Expression>>() {
				@Override
				public LinkedList<Expression> apply(final LinkedList<Expression> l, final HDLExpression e) {
					LinkedList<Expression> _xblockexpression = null;
					{
						final Expression _vHDL = VHDLExpressionExtension.this.toVHDL(e);
						l.add(_vHDL);
						_xblockexpression = l;
					}
					return _xblockexpression;
				}
			};
			final LinkedList<Expression> _fold = IterableExtensions.<HDLExpression, LinkedList<Expression>> fold(_ifArray_1, _linkedList, _function);
			final ArrayElement<Name> _arrayElement = new ArrayElement<Name>(result, _fold);
			result = _arrayElement;
		}
		return this.getRef(result, obj);
	}

	protected Expression _toVHDL(final HDLFunctionCall obj) {
		return VHDLFunctions.toOutputExpression(obj);
	}

	protected Signal _toVHDL(final HDLEnumRef obj) {
		final HDLEnum hEnum = obj.resolveHEnumForced("VHDL");
		final String _name = hEnum.getName();
		final String _plus = ("$" + _name);
		final String _plus_1 = (_plus + "_");
		final HDLQualifiedName _varRefName = obj.getVarRefName();
		final String _lastSegment = _varRefName.getLastSegment();
		final String _plus_2 = (_plus_1 + _lastSegment);
		final String _vHDLName = VHDLUtils.getVHDLName(_plus_2);
		return new Signal(_vHDLName, UnresolvedType.NO_NAME);
	}

	protected Expression _toVHDL(final HDLConcat obj) {
		final List<HDLExpression> cats = obj.getCats();
		final HDLExpression _get = cats.get(0);
		Expression res = this.toVHDL(_get);
		cats.remove(0);
		for (final HDLExpression cat : cats) {
			final Expression _vHDL = this.toVHDL(cat);
			final Concatenate _concatenate = new Concatenate(res, _vHDL);
			res = _concatenate;
		}
		return res;
	}

	protected Expression _toVHDL(final HDLManip obj) {
		final HDLManip.HDLManipType _type = obj.getType();
		final HDLManip.HDLManipType type = _type;
		boolean _matched = false;
		if (Objects.equal(type, HDLManip.HDLManipType.ARITH_NEG)) {
			_matched = true;
			final HDLExpression _target = obj.getTarget();
			final Expression _vHDL = this.toVHDL(_target);
			return new Minus(_vHDL);
		}
		if (!_matched) {
			if (((type == HDLManip.HDLManipType.LOGIC_NEG) || (type == HDLManip.HDLManipType.BIT_NEG))) {
				_matched = true;
				final HDLExpression _target_1 = obj.getTarget();
				final Expression _vHDL_1 = this.toVHDL(_target_1);
				return new Not(_vHDL_1);
			}
		}
		if (!_matched) {
			if (Objects.equal(type, HDLManip.HDLManipType.CAST)) {
				_matched = true;
				final HDLType _castTo = obj.getCastTo();
				final HDLPrimitive targetType = ((HDLPrimitive) _castTo);
				final HDLPrimitive.HDLPrimitiveType _type_1 = targetType.getType();
				final boolean _tripleEquals = (_type_1 == HDLPrimitive.HDLPrimitiveType.STRING);
				if (_tripleEquals) {
					final HDLExpression _target_2 = obj.getTarget();
					return this.toVHDL(_target_2);
				}
				final HDLExpression tWidth = targetType.getWidth();
				final HDLExpression _target_3 = obj.getTarget();
				final HDLClass _classType = _target_3.getClassType();
				final boolean _tripleEquals_1 = (_classType == HDLClass.HDLLiteral);
				if (_tripleEquals_1) {
					final IHDLObject _container = obj.getContainer();
					final HDLExpression _target_4 = obj.getTarget();
					return VHDLCastsLibrary.handleLiteral(_container, ((HDLLiteral) _target_4), targetType, tWidth);
				}
				final HDLExpression _target_5 = obj.getTarget();
				final HDLType _typeOfForced = TypeExtension.typeOfForced(_target_5, "VHDL");
				final HDLPrimitive t = ((HDLPrimitive) _typeOfForced);
				final HDLExpression _target_6 = obj.getTarget();
				Expression exp = this.toVHDL(_target_6);
				HDLPrimitive.HDLPrimitiveType actualType = t.getType();
				if ((tWidth != null)) {
					final VHDLCastsLibrary.TargetType resized = VHDLCastsLibrary.getResize(exp, t, tWidth);
					exp = resized.resized;
					actualType = resized.newType;
				}
				final HDLPrimitive.HDLPrimitiveType _type_2 = targetType.getType();
				return VHDLCastsLibrary.cast(exp, actualType, _type_2);
			}
		}
		throw new IllegalArgumentException(("Not supported:" + obj));
	}

	public Range toVHDL(final HDLRange obj, final Range.Direction dir) {
		final HDLEvaluationContext _hDLEvaluationContext = new HDLEvaluationContext();
		final Procedure1<HDLEvaluationContext> _function = new Procedure1<HDLEvaluationContext>() {
			@Override
			public void apply(final HDLEvaluationContext it) {
				it.ignoreConstantRefs = true;
				it.ignoreParameterRefs = true;
			}
		};
		final HDLEvaluationContext context = ObjectExtensions.<HDLEvaluationContext> operator_doubleArrow(_hDLEvaluationContext, _function);
		final HDLExpression _to = obj.getTo();
		final HDLExpression _simplifyWidth = HDLPrimitives.simplifyWidth(obj, _to, context);
		final Expression to = this.toVHDL(_simplifyWidth);
		final HDLExpression _from = obj.getFrom();
		final boolean _tripleEquals = (_from == null);
		if (_tripleEquals)
			return new Range(to, dir, to);
		final HDLExpression _from_1 = obj.getFrom();
		final HDLExpression _simplifyWidth_1 = HDLPrimitives.simplifyWidth(obj, _from_1, context);
		final Expression _vHDL = this.toVHDL(_simplifyWidth_1);
		return new Range(_vHDL, dir, to);
	}

	protected Literal _toVHDL(final HDLLiteral obj) {
		int length = (-1);
		final BigInteger _valueAsBigInt = obj.getValueAsBigInt();
		final boolean _tripleNotEquals = (_valueAsBigInt != null);
		if (_tripleNotEquals) {
			final BigInteger _valueAsBigInt_1 = obj.getValueAsBigInt();
			final int _bitLength = _valueAsBigInt_1.bitLength();
			length = _bitLength;
		}
		return this.toVHDL(obj, length, false);
	}

	public Literal toVHDL(final HDLLiteral obj, final int length, final boolean asString) {
		int l = length;
		final String sVal = obj.getVal();
		if ((l == 0)) {
			l = 1;
		}
		final BigInteger dec = obj.getValueAsBigInt();
		final HDLLiteral.HDLLiteralPresentation _presentation = obj.getPresentation();
		if (_presentation != null) {
			switch (_presentation) {
			case STR:
				return new StringLiteral(sVal);
			case BOOL:
				final boolean _equals = "true".equals(sVal);
				if (_equals)
					return Standard.BOOLEAN_TRUE;
				return Standard.BOOLEAN_FALSE;
			case HEX:
				if ((asString || (dec.bitLength() > 32)))
					return VHDLUtils.toHexLiteral(l, dec);
				final StringConcatenation _builder = new StringConcatenation();
				_builder.append("16#");
				final String _substring = sVal.substring(2);
				_builder.append(_substring, "");
				_builder.append("#");
				return new BasedLiteral(_builder.toString());
			case BIN:
				if ((asString || (dec.bitLength() > 32)))
					return VHDLUtils.toBinaryLiteral(l, dec);
				final StringConcatenation _builder_1 = new StringConcatenation();
				_builder_1.append("2#");
				final String _substring_1 = sVal.substring(2);
				_builder_1.append(_substring_1, "");
				_builder_1.append("#");
				return new BasedLiteral(_builder_1.toString());
			default:
				break;
			}
		}
		if (((dec.bitLength() > 31) || asString))
			return VHDLUtils.toBinaryLiteral(l, dec);
		return new DecimalLiteral(sVal);
	}

	protected Expression _toVHDL(final HDLShiftOp obj) {
		final HDLExpression _left = obj.getLeft();
		final HDLType _typeOfForced = TypeExtension.typeOfForced(_left, "VHDL");
		final HDLPrimitive type = ((HDLPrimitive) _typeOfForced);
		final HDLExpression _left_1 = obj.getLeft();
		final Expression _vHDL = this.toVHDL(_left_1);
		final HDLExpression _right = obj.getRight();
		final Expression _vHDL_1 = this.toVHDL(_right);
		final HDLPrimitive.HDLPrimitiveType _type = type.getType();
		final HDLShiftOp.HDLShiftOpType _type_1 = obj.getType();
		return VHDLShiftLibrary.shift(_vHDL, _vHDL_1, _type, _type_1);
	}

	protected Expression _toVHDL(final HDLEqualityOp obj) {
		final HDLEqualityOp.HDLEqualityOpType _type = obj.getType();
		if (_type != null) {
			switch (_type) {
			case EQ:
				final HDLExpression _left = obj.getLeft();
				final Expression _vHDL = this.toVHDL(_left);
				final HDLExpression _right = obj.getRight();
				final Expression _vHDL_1 = this.toVHDL(_right);
				final Equals _equals = new Equals(_vHDL, _vHDL_1);
				return new Parentheses(_equals);
			case GREATER_EQ:
				final HDLExpression _left_1 = obj.getLeft();
				final Expression _vHDL_2 = this.toVHDL(_left_1);
				final HDLExpression _right_1 = obj.getRight();
				final Expression _vHDL_3 = this.toVHDL(_right_1);
				final GreaterEquals _greaterEquals = new GreaterEquals(_vHDL_2, _vHDL_3);
				return new Parentheses(_greaterEquals);
			case GREATER:
				final HDLExpression _left_2 = obj.getLeft();
				final Expression _vHDL_4 = this.toVHDL(_left_2);
				final HDLExpression _right_2 = obj.getRight();
				final Expression _vHDL_5 = this.toVHDL(_right_2);
				final GreaterThan _greaterThan = new GreaterThan(_vHDL_4, _vHDL_5);
				return new Parentheses(_greaterThan);
			case LESS_EQ:
				final HDLExpression _left_3 = obj.getLeft();
				final Expression _vHDL_6 = this.toVHDL(_left_3);
				final HDLExpression _right_3 = obj.getRight();
				final Expression _vHDL_7 = this.toVHDL(_right_3);
				final LessEquals _lessEquals = new LessEquals(_vHDL_6, _vHDL_7);
				return new Parentheses(_lessEquals);
			case LESS:
				final HDLExpression _left_4 = obj.getLeft();
				final Expression _vHDL_8 = this.toVHDL(_left_4);
				final HDLExpression _right_4 = obj.getRight();
				final Expression _vHDL_9 = this.toVHDL(_right_4);
				final LessThan _lessThan = new LessThan(_vHDL_8, _vHDL_9);
				return new Parentheses(_lessThan);
			case NOT_EQ:
				final HDLExpression _left_5 = obj.getLeft();
				final Expression _vHDL_10 = this.toVHDL(_left_5);
				final HDLExpression _right_5 = obj.getRight();
				final Expression _vHDL_11 = this.toVHDL(_right_5);
				final NotEquals _notEquals = new NotEquals(_vHDL_10, _vHDL_11);
				return new Parentheses(_notEquals);
			default:
				break;
			}
		}
		throw new IllegalArgumentException(("Not supported:" + obj));
	}

	protected Expression _toVHDL(final HDLBitOp obj) {
		final HDLBitOp.HDLBitOpType _type = obj.getType();
		final HDLBitOp.HDLBitOpType type = _type;
		boolean _matched = false;
		if (((type == HDLBitOp.HDLBitOpType.AND) || (type == HDLBitOp.HDLBitOpType.LOGI_AND))) {
			_matched = true;
			final HDLExpression _left = obj.getLeft();
			final Expression _vHDL = this.toVHDL(_left);
			final HDLExpression _right = obj.getRight();
			final Expression _vHDL_1 = this.toVHDL(_right);
			final And _and = new And(_vHDL, _vHDL_1);
			return new Parentheses(_and);
		}
		if (!_matched) {
			if (((type == HDLBitOp.HDLBitOpType.OR) || (type == HDLBitOp.HDLBitOpType.LOGI_OR))) {
				_matched = true;
				final HDLExpression _left_1 = obj.getLeft();
				final Expression _vHDL_2 = this.toVHDL(_left_1);
				final HDLExpression _right_1 = obj.getRight();
				final Expression _vHDL_3 = this.toVHDL(_right_1);
				final Or _or = new Or(_vHDL_2, _vHDL_3);
				return new Parentheses(_or);
			}
		}
		if (!_matched) {
			if (Objects.equal(type, HDLBitOp.HDLBitOpType.XOR)) {
				_matched = true;
				final HDLExpression _left_2 = obj.getLeft();
				final Expression _vHDL_4 = this.toVHDL(_left_2);
				final HDLExpression _right_2 = obj.getRight();
				final Expression _vHDL_5 = this.toVHDL(_right_2);
				final Xor _xor = new Xor(_vHDL_4, _vHDL_5);
				return new Parentheses(_xor);
			}
		}
		throw new IllegalArgumentException(("Not supported:" + obj));
	}

	protected Expression _toVHDL(final HDLArithOp obj) {
		final HDLArithOp.HDLArithOpType _type = obj.getType();
		if (_type != null) {
			switch (_type) {
			case PLUS:
				final HDLExpression _left = obj.getLeft();
				final Expression _vHDL = this.toVHDL(_left);
				final HDLExpression _right = obj.getRight();
				final Expression _vHDL_1 = this.toVHDL(_right);
				final Add _add = new Add(_vHDL, _vHDL_1);
				return new Parentheses(_add);
			case MINUS:
				final HDLExpression _left_1 = obj.getLeft();
				final Expression _vHDL_2 = this.toVHDL(_left_1);
				final HDLExpression _right_1 = obj.getRight();
				final Expression _vHDL_3 = this.toVHDL(_right_1);
				final Subtract _subtract = new Subtract(_vHDL_2, _vHDL_3);
				return new Parentheses(_subtract);
			case DIV:
				final HDLExpression _left_2 = obj.getLeft();
				final Expression _vHDL_4 = this.toVHDL(_left_2);
				final HDLExpression _right_2 = obj.getRight();
				final Expression _vHDL_5 = this.toVHDL(_right_2);
				final Divide _divide = new Divide(_vHDL_4, _vHDL_5);
				return new Parentheses(_divide);
			case MUL:
				final HDLExpression _left_3 = obj.getLeft();
				final Expression _vHDL_6 = this.toVHDL(_left_3);
				final HDLExpression _right_3 = obj.getRight();
				final Expression _vHDL_7 = this.toVHDL(_right_3);
				final Multiply _multiply = new Multiply(_vHDL_6, _vHDL_7);
				return new Parentheses(_multiply);
			case MOD:
				final HDLExpression _left_4 = obj.getLeft();
				final Expression _vHDL_8 = this.toVHDL(_left_4);
				final HDLExpression _right_4 = obj.getRight();
				final Expression _vHDL_9 = this.toVHDL(_right_4);
				final Rem _rem = new Rem(_vHDL_8, _vHDL_9);
				return new Parentheses(_rem);
			case POW:
				final HDLExpression _left_5 = obj.getLeft();
				final Expression _vHDL_10 = this.toVHDL(_left_5);
				final HDLExpression _right_5 = obj.getRight();
				final Expression _vHDL_11 = this.toVHDL(_right_5);
				final Pow _pow = new Pow(_vHDL_10, _vHDL_11);
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
		final HDLExpression _ifExpr = obj.getIfExpr();
		final Expression _vHDL = this.toVHDL(_ifExpr);
		final AssociationElement _associationElement = new AssociationElement(_vHDL);
		parameters.add(_associationElement);
		final HDLExpression _thenExpr = obj.getThenExpr();
		final Expression _vHDL_1 = this.toVHDL(_thenExpr);
		final AssociationElement _associationElement_1 = new AssociationElement(_vHDL_1);
		parameters.add(_associationElement_1);
		final HDLExpression _elseExpr = obj.getElseExpr();
		final Expression _vHDL_2 = this.toVHDL(_elseExpr);
		final AssociationElement _associationElement_2 = new AssociationElement(_vHDL_2);
		parameters.add(_associationElement_2);
		return fc;
	}

	protected Expression _toVHDL(final HDLFunction obj) {
		throw new IllegalArgumentException(("Not supported:" + obj));
	}

	public Expression toVHDL(final IHDLObject obj) {
		if (obj instanceof HDLInterfaceRef)
			return _toVHDL((HDLInterfaceRef) obj);
		else if (obj instanceof HDLEnumRef)
			return _toVHDL((HDLEnumRef) obj);
		else if (obj instanceof HDLVariableRef)
			return _toVHDL((HDLVariableRef) obj);
		else if (obj instanceof HDLArithOp)
			return _toVHDL((HDLArithOp) obj);
		else if (obj instanceof HDLBitOp)
			return _toVHDL((HDLBitOp) obj);
		else if (obj instanceof HDLEqualityOp)
			return _toVHDL((HDLEqualityOp) obj);
		else if (obj instanceof HDLFunction)
			return _toVHDL((HDLFunction) obj);
		else if (obj instanceof HDLShiftOp)
			return _toVHDL((HDLShiftOp) obj);
		else if (obj instanceof HDLArrayInit)
			return _toVHDL((HDLArrayInit) obj);
		else if (obj instanceof HDLConcat)
			return _toVHDL((HDLConcat) obj);
		else if (obj instanceof HDLFunctionCall)
			return _toVHDL((HDLFunctionCall) obj);
		else if (obj instanceof HDLLiteral)
			return _toVHDL((HDLLiteral) obj);
		else if (obj instanceof HDLManip)
			return _toVHDL((HDLManip) obj);
		else if (obj instanceof HDLReference)
			return _toVHDL((HDLReference) obj);
		else if (obj instanceof HDLTernary)
			return _toVHDL((HDLTernary) obj);
		else if (obj instanceof HDLExpression)
			return _toVHDL((HDLExpression) obj);
		else
			throw new IllegalArgumentException("Unhandled parameter types: " + Arrays.<Object> asList(obj).toString());
	}

	public String getVHDLName(final HDLVariableRef obj) {
		if (obj instanceof HDLInterfaceRef)
			return _getVHDLName((HDLInterfaceRef) obj);
		else if (obj != null)
			return _getVHDLName(obj);
		else
			throw new IllegalArgumentException("Unhandled parameter types: " + Arrays.<Object> asList(obj).toString());
	}

	public Expression toVHDLArray(final HDLExpression obj, final Expression otherValue) {
		if (obj instanceof HDLArrayInit)
			return _toVHDLArray((HDLArrayInit) obj, otherValue);
		else if (obj != null)
			return _toVHDLArray(obj, otherValue);
		else
			throw new IllegalArgumentException("Unhandled parameter types: " + Arrays.<Object> asList(obj, otherValue).toString());
	}
}
