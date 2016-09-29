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
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.xtext.xbase.lib.CollectionExtensions;
import org.eclipse.xtext.xbase.lib.Conversions;
import org.eclipse.xtext.xbase.lib.Exceptions;
import org.eclipse.xtext.xbase.lib.Extension;
import org.eclipse.xtext.xbase.lib.Functions.Function1;
import org.eclipse.xtext.xbase.lib.IterableExtensions;
import org.eclipse.xtext.xbase.lib.ObjectExtensions;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure1;
import org.pshdl.generator.vhdl.libraries.VHDLCastsLibrary;
import org.pshdl.model.HDLAnnotation;
import org.pshdl.model.HDLArithOp;
import org.pshdl.model.HDLArrayInit;
import org.pshdl.model.HDLAssignment;
import org.pshdl.model.HDLBlock;
import org.pshdl.model.HDLClass;
import org.pshdl.model.HDLDirectGeneration;
import org.pshdl.model.HDLEnum;
import org.pshdl.model.HDLEnumDeclaration;
import org.pshdl.model.HDLEnumRef;
import org.pshdl.model.HDLExport;
import org.pshdl.model.HDLExpression;
import org.pshdl.model.HDLForLoop;
import org.pshdl.model.HDLFunction;
import org.pshdl.model.HDLFunctionCall;
import org.pshdl.model.HDLIfStatement;
import org.pshdl.model.HDLInterface;
import org.pshdl.model.HDLInterfaceDeclaration;
import org.pshdl.model.HDLInterfaceInstantiation;
import org.pshdl.model.HDLInterfaceRef;
import org.pshdl.model.HDLLiteral;
import org.pshdl.model.HDLObject;
import org.pshdl.model.HDLPrimitive;
import org.pshdl.model.HDLRange;
import org.pshdl.model.HDLReference;
import org.pshdl.model.HDLRegisterConfig;
import org.pshdl.model.HDLResolvedRef;
import org.pshdl.model.HDLStatement;
import org.pshdl.model.HDLSwitchCaseStatement;
import org.pshdl.model.HDLSwitchStatement;
import org.pshdl.model.HDLType;
import org.pshdl.model.HDLUnit;
import org.pshdl.model.HDLVariable;
import org.pshdl.model.HDLVariableDeclaration;
import org.pshdl.model.HDLVariableRef;
import org.pshdl.model.IHDLObject;
import org.pshdl.model.evaluation.ConstantEvaluate;
import org.pshdl.model.evaluation.HDLEvaluationContext;
import org.pshdl.model.extensions.FullNameExtension;
import org.pshdl.model.extensions.TypeExtension;
import org.pshdl.model.parser.SourceInfo;
import org.pshdl.model.types.builtIn.HDLBuiltInAnnotationProvider;
import org.pshdl.model.utils.HDLCodeGenerationException;
import org.pshdl.model.utils.HDLQualifiedName;
import org.pshdl.model.utils.HDLQuery;
import org.pshdl.model.utils.Insulin;

import com.google.common.base.Objects;
import com.google.common.base.Optional;

import de.upb.hni.vmagic.AssociationElement;
import de.upb.hni.vmagic.Choices;
import de.upb.hni.vmagic.DiscreteRange;
import de.upb.hni.vmagic.Range;
import de.upb.hni.vmagic.concurrent.ComponentInstantiation;
import de.upb.hni.vmagic.concurrent.ConcurrentStatement;
import de.upb.hni.vmagic.concurrent.EntityInstantiation;
import de.upb.hni.vmagic.concurrent.ForGenerateStatement;
import de.upb.hni.vmagic.declaration.Component;
import de.upb.hni.vmagic.declaration.ConstantDeclaration;
import de.upb.hni.vmagic.declaration.SignalDeclaration;
import de.upb.hni.vmagic.declaration.VariableDeclaration;
import de.upb.hni.vmagic.expression.Aggregate;
import de.upb.hni.vmagic.expression.Expression;
import de.upb.hni.vmagic.expression.Literal;
import de.upb.hni.vmagic.expression.TypeConversion;
import de.upb.hni.vmagic.libraryunit.Entity;
import de.upb.hni.vmagic.literal.CharacterLiteral;
import de.upb.hni.vmagic.object.Constant;
import de.upb.hni.vmagic.object.Signal;
import de.upb.hni.vmagic.object.SignalAssignmentTarget;
import de.upb.hni.vmagic.object.Variable;
import de.upb.hni.vmagic.object.VariableAssignmentTarget;
import de.upb.hni.vmagic.object.VhdlObject;
import de.upb.hni.vmagic.object.VhdlObjectProvider;
import de.upb.hni.vmagic.statement.CaseStatement;
import de.upb.hni.vmagic.statement.ForStatement;
import de.upb.hni.vmagic.statement.IfStatement;
import de.upb.hni.vmagic.statement.SequentialStatement;
import de.upb.hni.vmagic.statement.SignalAssignment;
import de.upb.hni.vmagic.statement.VariableAssignment;
import de.upb.hni.vmagic.type.ConstrainedArray;
import de.upb.hni.vmagic.type.EnumerationType;
import de.upb.hni.vmagic.type.IndexSubtypeIndication;
import de.upb.hni.vmagic.type.SubtypeIndication;
import de.upb.hni.vmagic.type.UnresolvedType;

@SuppressWarnings("all")
public class VHDLStatementExtension {
	public static VHDLStatementExtension INST = new VHDLStatementExtension();

	public static VHDLContext vhdlOf(final HDLStatement stmnt, final int pid) {
		return VHDLStatementExtension.INST.toVHDL(stmnt, pid);
	}

	@Extension
	private final VHDLExpressionExtension vee = new VHDLExpressionExtension();

	private static HDLObject.GenericMeta<HDLQualifiedName> ORIGINAL_FULLNAME = new HDLObject.GenericMeta<HDLQualifiedName>("ORIGINAL_FULLNAME", true);

	public static HDLObject.GenericMeta<Boolean> EXPORT = new HDLObject.GenericMeta<Boolean>("EXPORT", true);

	protected VHDLContext _toVHDL(final IHDLObject obj, final int pid) {
		final HDLClass _classType = obj.getClassType();
		final String _plus = ("Not correctly implemented:" + _classType);
		final String _plus_1 = (_plus + " ");
		final String _plus_2 = (_plus_1 + obj);
		throw new IllegalArgumentException(_plus_2);
	}

	protected VHDLContext _toVHDL(final HDLExport obj, final int pid) {
		final VHDLContext res = new VHDLContext();
		final Optional<HDLInterfaceRef> _interfaceRef = obj.toInterfaceRef();
		final HDLInterfaceRef _get = _interfaceRef.get();
		final HDLVariable hVar = _get.resolveVarForced("VHDL");
		final HDLVariableDeclaration _container = hVar.<HDLVariableDeclaration> getContainer(HDLVariableDeclaration.class);
		final VHDLContext _vHDL = this.toVHDL(_container, pid);
		res.merge(_vHDL, false);
		return res;
	}

	protected VHDLContext _toVHDL(final HDLDirectGeneration obj, final int pid) {
		return new VHDLContext();
	}

	protected VHDLContext _toVHDL(final HDLFunctionCall obj, final int pid) {
		return VHDLFunctions.toOutputStatement(obj, pid, null);
	}

	protected VHDLContext _toVHDL(final HDLBlock obj, final int pid) {
		final VHDLContext res = new VHDLContext();
		boolean process = false;
		if (((obj.getProcess() != null) && (obj.getProcess()).booleanValue())) {
			process = true;
		}
		int _xifexpression = 0;
		if (process) {
			_xifexpression = res.newProcessID();
		} else {
			_xifexpression = pid;
		}
		final int newPid = _xifexpression;
		final ArrayList<HDLStatement> _statements = obj.getStatements();
		for (final HDLStatement stmnt : _statements) {
			{
				final VHDLContext vhdl = this.toVHDL(stmnt, newPid);
				if ((vhdl == null))
					throw new HDLCodeGenerationException(stmnt, "No VHDL code could be generated", "VHDL");
				res.merge(vhdl, false);
			}
		}
		return this.attachComment(res, obj);
	}

	public VHDLContext attachComment(final VHDLContext context, final IHDLObject block) {
		try {
			final SourceInfo srcInfo = block.<SourceInfo> getMeta(SourceInfo.INFO);
			if ((srcInfo != null)) {
				final ArrayList<String> newComments = new ArrayList<String>();
				final ArrayList<String> docComments = new ArrayList<String>();
				for (final String comment : srcInfo.comments) {
					final boolean _startsWith = comment.startsWith("//");
					if (_startsWith) {
						final int _length = comment.length();
						final int _minus = (_length - 1);
						final String newComment = comment.substring(2, _minus);
						final boolean _startsWith_1 = newComment.startsWith("/");
						if (_startsWith_1) {
							final boolean _startsWith_2 = newComment.startsWith("/<");
							if (_startsWith_2) {
								final String _substring = newComment.substring(2);
								docComments.add(_substring);
							} else {
								final String _substring_1 = newComment.substring(1);
								docComments.add(_substring_1);
							}
						} else {
							newComments.add(newComment);
						}
					} else {
						final int _length_1 = comment.length();
						final int _minus_1 = (_length_1 - 2);
						final String newComment_1 = comment.substring(2, _minus_1);
						final boolean _startsWith_3 = newComment_1.startsWith("*");
						if (_startsWith_3) {
							final boolean _startsWith_4 = newComment_1.startsWith("*<");
							if (_startsWith_4) {
								final String _substring_2 = newComment_1.substring(2);
								final String[] _split = _substring_2.split("\n");
								CollectionExtensions.<String> addAll(docComments, _split);
							} else {
								final String _substring_3 = newComment_1.substring(1);
								final String[] _split_1 = _substring_3.split("\n");
								CollectionExtensions.<String> addAll(docComments, _split_1);
							}
						} else {
							final String[] _split_2 = newComment_1.split("\n");
							CollectionExtensions.<String> addAll(newComments, _split_2);
						}
					}
				}
				if (((!newComments.isEmpty()) || (!docComments.isEmpty()))) {
					context.attachComments(newComments, docComments);
				}
			}
		} catch (final Throwable _t) {
			if (_t instanceof Exception) {
				final Exception e = (Exception) _t;
			} else
				throw Exceptions.sneakyThrow(_t);
		}
		return context;
	}

	protected VHDLContext _toVHDL(final HDLEnumDeclaration obj, final int pid) {
		final VHDLContext res = new VHDLContext();
		final HDLEnum hEnum = obj.getHEnum();
		final List<String> enums = new LinkedList<String>();
		final ArrayList<HDLVariable> _enums = hEnum.getEnums();
		for (final HDLVariable hVar : _enums) {
			final String _name = hEnum.getName();
			final String _plus = ("$" + _name);
			final String _plus_1 = (_plus + "_");
			final String _name_1 = hVar.getName();
			final String _plus_2 = (_plus_1 + _name_1);
			final String _vHDLName = VHDLUtils.getVHDLName(_plus_2);
			enums.add(_vHDLName);
		}
		final String[] enumArr = ((String[]) Conversions.unwrapArray(enums, String.class));
		final String _name_2 = hEnum.getName();
		final String _plus_3 = ("$enum_" + _name_2);
		final String _vHDLName_1 = VHDLUtils.getVHDLName(_plus_3);
		final EnumerationType _enumerationType = new EnumerationType(_vHDLName_1, enumArr);
		res.addTypeDeclaration(_enumerationType, false);
		return this.attachComment(res, obj);
	}

	protected VHDLContext _toVHDL(final HDLInterfaceDeclaration obj, final int pid) {
		return new VHDLContext();
	}

	private static EnumSet<HDLVariableDeclaration.HDLDirection> inAndOut = EnumSet.<HDLVariableDeclaration.HDLDirection> of(HDLVariableDeclaration.HDLDirection.IN,
			HDLVariableDeclaration.HDLDirection.INOUT, HDLVariableDeclaration.HDLDirection.OUT);

	protected VHDLContext _toVHDL(final HDLInterfaceInstantiation hii, final int pid) {
		final VHDLContext res = new VHDLContext();
		final HDLInterface hIf = hii.resolveHIfForced("VHDL");
		final HDLVariable interfaceVar = hii.getVar();
		final HDLVariable _var = hii.getVar();
		final String ifName = _var.getName();
		final HDLQualifiedName asRef = hIf.asRef();
		final HDLInterfaceDeclaration hid = hIf.<HDLInterfaceDeclaration> getContainer(HDLInterfaceDeclaration.class);
		List<AssociationElement> portMap = null;
		List<AssociationElement> genericMap = null;
		ConcurrentStatement instantiation = null;
		final ArrayList<HDLVariableDeclaration> ports = hIf.getPorts();
		if (((hid != null) && (hid.getAnnotation(HDLBuiltInAnnotationProvider.HDLBuiltInAnnotations.VHDLComponent) != null))) {
			final HDLAnnotation anno = hid.getAnnotation(HDLBuiltInAnnotationProvider.HDLBuiltInAnnotations.VHDLComponent);
			String _value = null;
			if (anno != null) {
				_value = anno.getValue();
			}
			final boolean _equals = "declare".equals(_value);
			if (_equals) {
				final String _lastSegment = asRef.getLastSegment();
				final String _string = _lastSegment.toString();
				final Component c = new Component(_string);
				final VHDLContext cContext = new VHDLContext();
				for (final HDLVariableDeclaration port : ports) {
					final VHDLContext _vHDL = this.toVHDL(port, (-1));
					cContext.merge(_vHDL, true);
				}
				for (final Signal signal : cContext.ports) {
					final List<VhdlObjectProvider<Signal>> _port = c.getPort();
					_port.add(signal);
				}
				for (final ConstantDeclaration cd : cContext.constants) {
					final List<Constant> _objects = cd.getObjects();
					for (final Object vobj : _objects) {
						final List<VhdlObjectProvider<Constant>> _generic = c.getGeneric();
						_generic.add(((Constant) vobj));
					}
				}
				for (final Constant constant : cContext.generics) {
					final List<VhdlObjectProvider<Constant>> _generic_1 = c.getGeneric();
					_generic_1.add(constant);
				}
				res.addComponent(c);
			} else {
				final HDLQualifiedName _nameRef = VHDLPackageExtension.INST.getNameRef(asRef);
				res.addImport(_nameRef);
			}
			final String _lastSegment_1 = asRef.getLastSegment();
			final String _string_1 = _lastSegment_1.toString();
			final Component entity = new Component(_string_1);
			final ComponentInstantiation inst = new ComponentInstantiation(ifName, entity);
			final List<AssociationElement> _portMap = inst.getPortMap();
			portMap = _portMap;
			final List<AssociationElement> _genericMap = inst.getGenericMap();
			genericMap = _genericMap;
			instantiation = inst;
		} else {
			final HDLQualifiedName _nameRef_1 = VHDLPackageExtension.INST.getNameRef(asRef);
			final String _string_2 = _nameRef_1.toString();
			final Entity entity_1 = new Entity(_string_2);
			final EntityInstantiation inst_1 = new EntityInstantiation(ifName, entity_1);
			final List<AssociationElement> _portMap_1 = inst_1.getPortMap();
			portMap = _portMap_1;
			final List<AssociationElement> _genericMap_1 = inst_1.getGenericMap();
			genericMap = _genericMap_1;
			instantiation = inst_1;
		}
		final HDLUnit unit = hii.<HDLUnit> getContainer(HDLUnit.class);
		final HDLExport[] exportStmnts = unit.<HDLExport> getAllObjectsOf(HDLExport.class, true);
		final HDLExport[] _converted_exportStmnts = exportStmnts;
		final Function1<HDLExport, Boolean> _function = new Function1<HDLExport, Boolean>() {
			@Override
			public Boolean apply(final HDLExport it) {
				final HDLQualifiedName _varRefName = it.getVarRefName();
				return Boolean.valueOf((_varRefName != null));
			}
		};
		final Iterable<HDLExport> _filter = IterableExtensions.<HDLExport> filter(((Iterable<HDLExport>) Conversions.doWrapArray(_converted_exportStmnts)), _function);
		final Function1<HDLExport, String> _function_1 = new Function1<HDLExport, String>() {
			@Override
			public String apply(final HDLExport e) {
				final HDLQualifiedName _varRefName = e.getVarRefName();
				return _varRefName.getLastSegment();
			}
		};
		final Iterable<String> _map = IterableExtensions.<HDLExport, String> map(_filter, _function_1);
		final Set<String> exportedSignals = IterableExtensions.<String> toSet(_map);
		for (final HDLVariableDeclaration hvd : ports) {
			final HDLVariableDeclaration.HDLDirection _direction = hvd.getDirection();
			final boolean _contains = VHDLStatementExtension.inAndOut.contains(_direction);
			if (_contains) {
				this.generatePortMap(hvd, ifName, interfaceVar, asRef, res, hii, pid, portMap, exportedSignals);
			} else {
				final HDLVariableDeclaration.HDLDirection _direction_1 = hvd.getDirection();
				final boolean _equals_1 = Objects.equal(_direction_1, HDLVariableDeclaration.HDLDirection.PARAMETER);
				if (_equals_1) {
					final ArrayList<HDLVariable> _variables = hvd.getVariables();
					for (final HDLVariable hvar : _variables) {
						{
							HDLVariable sigVar = hvar;
							final String _meta = hvar.<String> getMeta(HDLInterfaceInstantiation.ORIG_NAME);
							final boolean _tripleNotEquals = (_meta != null);
							if (_tripleNotEquals) {
								final String _meta_1 = hvar.<String> getMeta(HDLInterfaceInstantiation.ORIG_NAME);
								final HDLVariable _setName = hvar.setName(_meta_1);
								sigVar = _setName;
							}
							final HDLVariableRef ref = hvar.asHDLRef();
							final String _name = sigVar.getName();
							final Expression _vHDL_1 = this.vee.toVHDL(ref);
							final AssociationElement _associationElement = new AssociationElement(_name, _vHDL_1);
							genericMap.add(_associationElement);
						}
					}
				}
			}
		}
		ForGenerateStatement forLoop = null;
		final ArrayList<HDLExpression> _dimensions = interfaceVar.getDimensions();
		final int _size = _dimensions.size();
		final boolean _equals_2 = (_size == 0);
		if (_equals_2) {
			res.addConcurrentStatement(instantiation);
		} else {
			int i = 0;
			final ArrayList<HDLExpression> _dimensions_1 = interfaceVar.getDimensions();
			for (final HDLExpression exp : _dimensions_1) {
				{
					final ArrayList<HDLExpression> _dimensions_2 = interfaceVar.getDimensions();
					final HDLExpression _get = _dimensions_2.get(i);
					final HDLExpression to = HDLArithOp.subtract(_get, 1);
					final HDLRange _hDLRange = new HDLRange();
					final HDLLiteral _get_1 = HDLLiteral.get(0);
					final HDLRange _setFrom = _hDLRange.setFrom(_get_1);
					final HDLRange _setTo = _setFrom.setTo(to);
					final HDLRange range = _setTo.setContainer(hii);
					final String _asIndex = this.asIndex(Integer.valueOf(i));
					final Range _vHDL_1 = this.vee.toVHDL(range, Range.Direction.TO);
					final ForGenerateStatement newFor = new ForGenerateStatement(("generate_" + ifName), _asIndex, _vHDL_1);
					if ((forLoop != null)) {
						final List<ConcurrentStatement> _statements = forLoop.getStatements();
						_statements.add(newFor);
					} else {
						res.addConcurrentStatement(newFor);
					}
					forLoop = newFor;
					i = (i + 1);
				}
			}
			if ((forLoop == null))
				throw new IllegalArgumentException("Should not get here");
			final List<ConcurrentStatement> _statements = forLoop.getStatements();
			_statements.add(instantiation);
		}
		return this.attachComment(res, hii);
	}

	public void generatePortMap(final HDLVariableDeclaration hvd, final String ifName, final HDLVariable interfaceVar, final HDLQualifiedName asRef, final VHDLContext res,
			final HDLInterfaceInstantiation obj, final int pid, final List<AssociationElement> portMap, final Set<String> exportedSignals) {
		final HDLQuery.Source<HDLAnnotation> _select = HDLQuery.<HDLAnnotation> select(HDLAnnotation.class);
		final HDLQuery.Selector<HDLAnnotation> _from = _select.from(hvd);
		final HDLQuery.FieldSelector<HDLAnnotation, String> _where = _from.<String> where(HDLAnnotation.fName);
		final String _string = HDLBuiltInAnnotationProvider.HDLBuiltInAnnotations.VHDLType.toString();
		final HDLQuery.Result<HDLAnnotation, String> _isEqualTo = _where.isEqualTo(_string);
		final Collection<HDLAnnotation> typeAnno = _isEqualTo.getAll();
		final ArrayList<HDLVariable> _variables = hvd.getVariables();
		for (final HDLVariable hvar : _variables) {
			{
				HDLVariable sigVar = null;
				final String _name = hvar.getName();
				final boolean _contains = exportedSignals.contains(_name);
				if (_contains) {
					final HDLVariable _hDLVariable = new HDLVariable();
					final String _name_1 = hvar.getName();
					final HDLVariable _setName = _hDLVariable.setName(_name_1);
					sigVar = _setName;
					final HDLVariableRef ref = sigVar.asHDLRef();
					final String _name_2 = hvar.getName();
					final String _vHDLName = VHDLUtils.getVHDLName(_name_2);
					final Expression _vHDL = this.vee.toVHDL(ref);
					final AssociationElement _associationElement = new AssociationElement(_vHDLName, _vHDL);
					portMap.add(_associationElement);
				} else {
					final String _name_3 = hvar.getName();
					final String _mapName = VHDLUtils.mapName(ifName, _name_3);
					final HDLVariable _setName_1 = hvar.setName(_mapName);
					sigVar = _setName_1;
					HDLVariableRef ref_1 = sigVar.asHDLRef();
					int i = 0;
					final ArrayList<HDLExpression> _dimensions = interfaceVar.getDimensions();
					for (final HDLExpression exp : _dimensions) {
						{
							final HDLVariableRef _hDLVariableRef = new HDLVariableRef();
							final String _asIndex = this.asIndex(Integer.valueOf(i));
							final HDLQualifiedName _create = HDLQualifiedName.create(_asIndex);
							final HDLVariableRef _setVar = _hDLVariableRef.setVar(_create);
							final HDLVariableRef _addArray = ref_1.addArray(_setVar);
							ref_1 = _addArray;
							i = (i + 1);
						}
					}
					final ArrayList<HDLExpression> _dimensions_1 = interfaceVar.getDimensions();
					for (final HDLExpression exp_1 : _dimensions_1) {
						final HDLVariable _addDimensions = sigVar.addDimensions(exp_1);
						sigVar = _addDimensions;
					}
					final ArrayList<HDLExpression> _dimensions_2 = hvar.getDimensions();
					final int _size = _dimensions_2.size();
					final boolean _notEquals = (_size != 0);
					if (_notEquals) {
						final boolean _isEmpty = typeAnno.isEmpty();
						if (_isEmpty) {
							final HDLQualifiedName _packageNameRef = VHDLPackageExtension.INST.getPackageNameRef(asRef);
							final String _arrayRefName = VHDLStatementExtension.getArrayRefName(hvar, true);
							final HDLQualifiedName name = _packageNameRef.append(_arrayRefName);
							res.addImport(name);
							final HDLVariableDeclaration _setDirection = hvd.setDirection(HDLVariableDeclaration.HDLDirection.INTERNAL);
							final HDLVariable _setDimensions = sigVar.setDimensions(null);
							final String _string_1 = name.toString();
							final HDLAnnotation _create = HDLBuiltInAnnotationProvider.HDLBuiltInAnnotations.VHDLType.create(_string_1);
							final HDLVariable _addAnnotations = _setDimensions.addAnnotations(_create);
							final Iterable<HDLVariable> _asList = HDLObject.<HDLVariable> asList(_addAnnotations);
							final HDLVariableDeclaration _setVariables = _setDirection.setVariables(_asList);
							final HDLVariableDeclaration newHVD = _setVariables.copyDeepFrozen(obj);
							final VHDLContext _vHDL_1 = this.toVHDL(newHVD, pid);
							res.merge(_vHDL_1, false);
						} else {
							final HDLVariableDeclaration _setDirection_1 = hvd.setDirection(HDLVariableDeclaration.HDLDirection.INTERNAL);
							final HDLVariable _setDimensions_1 = sigVar.setDimensions(null);
							final Iterable<HDLVariable> _asList_1 = HDLObject.<HDLVariable> asList(_setDimensions_1);
							final HDLVariableDeclaration _setVariables_1 = _setDirection_1.setVariables(_asList_1);
							final HDLVariableDeclaration newHVD_1 = _setVariables_1.copyDeepFrozen(obj);
							final VHDLContext _vHDL_2 = this.toVHDL(newHVD_1, pid);
							res.merge(_vHDL_2, false);
						}
					} else {
						final HDLVariableDeclaration _setDirection_2 = hvd.setDirection(HDLVariableDeclaration.HDLDirection.INTERNAL);
						final Iterable<HDLVariable> _asList_2 = HDLObject.<HDLVariable> asList(sigVar);
						final HDLVariableDeclaration _setVariables_2 = _setDirection_2.setVariables(_asList_2);
						final HDLVariableDeclaration newHVD_2 = _setVariables_2.copyDeepFrozen(obj);
						final VHDLContext _vHDL_3 = this.toVHDL(newHVD_2, pid);
						res.merge(_vHDL_3, false);
					}
					final String _name_4 = hvar.getName();
					final String _vHDLName_1 = VHDLUtils.getVHDLName(_name_4);
					final Expression _vHDL_4 = this.vee.toVHDL(ref_1);
					final AssociationElement _associationElement_1 = new AssociationElement(_vHDLName_1, _vHDL_4);
					portMap.add(_associationElement_1);
				}
			}
		}
	}

	public String asIndex(final Integer integer) {
		final int i = "I".charAt(0);
		return Character.toString(((char) (i + (integer).intValue())));
	}

	public static String getArrayRefName(final HDLVariable hvar, final boolean external) {
		String res = null;
		if (external) {
			HDLQualifiedName fullName = null;
			final HDLQualifiedName _meta = hvar.<HDLQualifiedName> getMeta(VHDLStatementExtension.ORIGINAL_FULLNAME);
			final boolean _tripleNotEquals = (_meta != null);
			if (_tripleNotEquals) {
				final HDLQualifiedName _meta_1 = hvar.<HDLQualifiedName> getMeta(VHDLStatementExtension.ORIGINAL_FULLNAME);
				fullName = _meta_1;
			} else {
				final HDLQualifiedName _fullNameOf = FullNameExtension.fullNameOf(hvar);
				fullName = _fullNameOf;
			}
			final String _string = fullName.toString('_');
			res = _string;
		} else {
			final String _name = hvar.getName();
			res = _name;
		}
		final String _unescapeVHDLName = VHDLUtils.unescapeVHDLName(res);
		final String _plus = (_unescapeVHDLName + "_array");
		return VHDLUtils.getVHDLName(_plus);
	}

	protected VHDLContext _toVHDL(final HDLVariableDeclaration obj, final int pid) {
		final VHDLContext res = new VHDLContext();
		final HDLPrimitive primitive = obj.getPrimitive();
		SubtypeIndication type = null;
		HDLExpression resetValue = null;
		final HDLQuery.Source<HDLAnnotation> _select = HDLQuery.<HDLAnnotation> select(HDLAnnotation.class);
		final HDLQuery.Selector<HDLAnnotation> _from = _select.from(obj);
		final HDLQuery.FieldSelector<HDLAnnotation, String> _where = _from.<String> where(HDLAnnotation.fName);
		final String _string = HDLBuiltInAnnotationProvider.HDLBuiltInAnnotations.memory.toString();
		final HDLQuery.Result<HDLAnnotation, String> _isEqualTo = _where.isEqualTo(_string);
		final HDLAnnotation memAnno = _isEqualTo.getFirst();
		if ((memAnno != null))
			return res;
		final HDLQuery.Source<HDLAnnotation> _select_1 = HDLQuery.<HDLAnnotation> select(HDLAnnotation.class);
		final HDLQuery.Selector<HDLAnnotation> _from_1 = _select_1.from(obj);
		final HDLQuery.FieldSelector<HDLAnnotation, String> _where_1 = _from_1.<String> where(HDLAnnotation.fName);
		final String _string_1 = HDLBuiltInAnnotationProvider.HDLBuiltInAnnotations.VHDLType.toString();
		final HDLQuery.Result<HDLAnnotation, String> _isEqualTo_1 = _where_1.isEqualTo(_string_1);
		final HDLAnnotation typeAnno = _isEqualTo_1.getFirst();
		final HDLRegisterConfig _register = obj.getRegister();
		final boolean _tripleNotEquals = (_register != null);
		if (_tripleNotEquals) {
			final HDLRegisterConfig _register_1 = obj.getRegister();
			final HDLExpression _resetValue = _register_1.getResetValue();
			resetValue = _resetValue;
		}
		final char _charAt = "0".charAt(0);
		final CharacterLiteral _characterLiteral = new CharacterLiteral(_charAt);
		Expression otherValue = Aggregate.OTHERS(_characterLiteral);
		if ((typeAnno != null)) {
			final String typeValue = typeAnno.getValue();
			final boolean _endsWith = typeValue.endsWith("<>");
			if (_endsWith) {
				final int _length = typeValue.length();
				final int _minus = (_length - 2);
				final String _substring = typeValue.substring(0, _minus);
				final HDLQualifiedName value = new HDLQualifiedName(_substring);
				res.addImport(value);
				final String _lastSegment = value.getLastSegment();
				final EnumerationType _enumerationType = new EnumerationType(_lastSegment);
				type = _enumerationType;
				HDLRange range = null;
				final HDLExpression width = primitive.getWidth();
				final boolean _notEquals = (!Objects.equal(width, null));
				if (_notEquals) {
					final HDLRange _hDLRange = new HDLRange();
					final HDLArithOp _subtract = HDLArithOp.subtract(width, 1);
					final HDLRange _setFrom = _hDLRange.setFrom(_subtract);
					final HDLLiteral _get = HDLLiteral.get(0);
					final HDLRange _setTo = _setFrom.setTo(_get);
					range = _setTo;
					final HDLRange _copyDeepFrozen = range.copyDeepFrozen(obj);
					range = _copyDeepFrozen;
					final Range _vHDL = this.vee.toVHDL(range, Range.Direction.DOWNTO);
					final IndexSubtypeIndication _indexSubtypeIndication = new IndexSubtypeIndication(type, _vHDL);
					type = _indexSubtypeIndication;
				}
			} else {
				final HDLQualifiedName value_1 = new HDLQualifiedName(typeValue);
				res.addImport(value_1);
				final String _lastSegment_1 = value_1.getLastSegment();
				final EnumerationType _enumerationType_1 = new EnumerationType(_lastSegment_1);
				type = _enumerationType_1;
			}
		} else {
			if ((primitive != null)) {
				final SubtypeIndication _type = VHDLCastsLibrary.getType(primitive);
				type = _type;
			} else {
				final HDLType resolved = obj.resolveTypeForced("VHDL");
				if ((resolved instanceof HDLEnum)) {
					final HDLEnum hEnum = ((HDLEnum) resolved);
					final String _name = hEnum.getName();
					final String _plus = ("$enum_" + _name);
					final String _vHDLName = VHDLUtils.getVHDLName(_plus);
					final EnumerationType _enumerationType_2 = new EnumerationType(_vHDLName);
					type = _enumerationType_2;
					int idx = 0;
					final HDLEvaluationContext _hDLEvaluationContext = new HDLEvaluationContext();
					final Procedure1<HDLEvaluationContext> _function = new Procedure1<HDLEvaluationContext>() {
						@Override
						public void apply(final HDLEvaluationContext it) {
							it.enumAsInt = true;
						}
					};
					final HDLEvaluationContext _doubleArrow = ObjectExtensions.<HDLEvaluationContext> operator_doubleArrow(_hDLEvaluationContext, _function);
					final Optional<BigInteger> resVal = ConstantEvaluate.valueOf(resetValue, _doubleArrow);
					final boolean _isPresent = resVal.isPresent();
					if (_isPresent) {
						final BigInteger _get_1 = resVal.get();
						final int _intValue = _get_1.intValue();
						idx = _intValue;
					}
					final HDLEnumRef _hDLEnumRef = new HDLEnumRef();
					final HDLQualifiedName _asRef = hEnum.asRef();
					final HDLEnumRef _setHEnum = _hDLEnumRef.setHEnum(_asRef);
					final ArrayList<HDLVariable> _enums = hEnum.getEnums();
					final HDLVariable _get_2 = _enums.get(idx);
					final HDLQualifiedName _asRef_1 = _get_2.asRef();
					final HDLEnumRef enumReset = _setHEnum.setVar(_asRef_1);
					enumReset.freeze(hEnum);
					final Expression _vHDL_1 = this.vee.toVHDL(enumReset);
					otherValue = _vHDL_1;
					if ((!(resetValue instanceof HDLArrayInit))) {
						resetValue = enumReset;
					}
				}
			}
		}
		if ((type != null)) {
			final ArrayList<HDLVariable> _variables = obj.getVariables();
			for (final HDLVariable hvar : _variables) {
				this.handleVariable(hvar, type, obj, res, resetValue, otherValue, pid);
			}
		}
		return this.attachComment(res, obj);
	}

	public void handleVariable(final HDLVariable hvar, final SubtypeIndication type, final HDLVariableDeclaration obj, final VHDLContext res, final HDLExpression resetValue,
			final Expression otherValue, final int pid) {
		final boolean noExplicitResetVar = ((hvar.getAnnotation(HDLBuiltInAnnotationProvider.HDLBuiltInAnnotations.VHDLNoExplicitReset) != null)
				|| (hvar.getAnnotation(HDLBuiltInAnnotationProvider.HDLBuiltInAnnotations.memory) != null));
		SubtypeIndication varType = type;
		final ArrayList<HDLExpression> _dimensions = hvar.getDimensions();
		final int _size = _dimensions.size();
		final boolean _notEquals = (_size != 0);
		if (_notEquals) {
			final LinkedList<DiscreteRange> ranges = new LinkedList<DiscreteRange>();
			final ArrayList<HDLExpression> _dimensions_1 = hvar.getDimensions();
			for (final HDLExpression arrayWidth : _dimensions_1) {
				{
					final HDLExpression newWidth = HDLArithOp.subtract(arrayWidth, 1);
					final HDLRange _hDLRange = new HDLRange();
					final HDLLiteral _get = HDLLiteral.get(0);
					final HDLRange _setFrom = _hDLRange.setFrom(_get);
					final HDLRange _setTo = _setFrom.setTo(newWidth);
					final HDLRange _copyDeepFrozen = _setTo.copyDeepFrozen(obj);
					final Range range = this.vee.toVHDL(_copyDeepFrozen, Range.Direction.TO);
					ranges.add(range);
				}
			}
			final boolean external = obj.isExternal();
			final String _arrayRefName = VHDLStatementExtension.getArrayRefName(hvar, external);
			final ConstrainedArray arrType = new ConstrainedArray(_arrayRefName, type, ranges);
			res.addTypeDeclaration(arrType, external);
			varType = arrType;
		}
		String name = hvar.getName();
		final String _meta = hvar.<String> getMeta(HDLInterfaceInstantiation.ORIG_NAME);
		final boolean _notEquals_1 = (!Objects.equal(_meta, null));
		if (_notEquals_1) {
			final String _meta_1 = hvar.<String> getMeta(HDLInterfaceInstantiation.ORIG_NAME);
			name = _meta_1;
		}
		final String _name = hvar.getName();
		final Signal s = new Signal(_name, varType);
		if ((((resetValue != null) && (!noExplicitResetVar)) && (obj.getRegister() != null))) {
			boolean synchedArray = false;
			if ((resetValue instanceof HDLVariableRef)) {
				final HDLVariableRef ref = ((HDLVariableRef) resetValue);
				final Optional<HDLVariable> _resolveVar = ref.resolveVar();
				final HDLVariable _get = _resolveVar.get();
				final ArrayList<HDLExpression> _dimensions_2 = _get.getDimensions();
				final int _size_1 = _dimensions_2.size();
				final boolean _notEquals_2 = (_size_1 != 0);
				synchedArray = _notEquals_2;
			}
			final HDLVariableRef _hDLVariableRef = new HDLVariableRef();
			final HDLQualifiedName _asRef = hvar.asRef();
			final HDLVariableRef target = _hDLVariableRef.setVar(_asRef);
			if ((resetValue instanceof HDLArrayInit)) {
				final Expression _vHDLArray = this.vee.toVHDLArray(resetValue, otherValue);
				final SignalAssignment sa = new SignalAssignment(s, _vHDLArray);
				final HDLRegisterConfig _register = obj.getRegister();
				res.addResetValue(_register, sa);
			} else {
				final List<HDLExpression> _emptyList = Collections.<HDLExpression> emptyList();
				final ArrayList<HDLExpression> _dimensions_3 = hvar.getDimensions();
				final HDLStatement _createArrayForLoop = Insulin.createArrayForLoop(_emptyList, _dimensions_3, 0, resetValue, target, synchedArray);
				final HDLStatement initLoop = _createArrayForLoop.copyDeepFrozen(obj);
				final VHDLContext vhdl = this.toVHDL(initLoop, pid);
				final HDLRegisterConfig _register_1 = obj.getRegister();
				final SequentialStatement _statement = vhdl.getStatement();
				res.addResetValue(_register_1, _statement);
			}
		}
		final Constant constant = new Constant(name, varType);
		final HDLExpression _defaultValue = hvar.getDefaultValue();
		final boolean _tripleNotEquals = (_defaultValue != null);
		if (_tripleNotEquals) {
			final HDLExpression _defaultValue_1 = hvar.getDefaultValue();
			final Expression _vHDLArray_1 = this.vee.toVHDLArray(_defaultValue_1, otherValue);
			constant.setDefaultValue(_vHDLArray_1);
		}
		if (noExplicitResetVar) {
			if ((resetValue instanceof HDLArrayInit)) {
				final Expression _vHDLArray_2 = this.vee.toVHDLArray(resetValue, otherValue);
				s.setDefaultValue(_vHDLArray_2);
			} else {
				if ((resetValue != null)) {
					Expression assign = this.vee.toVHDL(resetValue);
					final ArrayList<HDLExpression> _dimensions_4 = hvar.getDimensions();
					for (final HDLExpression exp : _dimensions_4) {
						final Aggregate _OTHERS = Aggregate.OTHERS(assign);
						assign = _OTHERS;
					}
					s.setDefaultValue(assign);
				}
			}
		}
		final HDLVariableDeclaration.HDLDirection _direction = obj.getDirection();
		boolean _matched = false;
		if (Objects.equal(_direction, HDLVariableDeclaration.HDLDirection.IN)) {
			_matched = true;
			s.setMode(VhdlObject.Mode.IN);
			res.addPortDeclaration(s);
		}
		if (!_matched) {
			if (Objects.equal(_direction, HDLVariableDeclaration.HDLDirection.OUT)) {
				_matched = true;
				s.setMode(VhdlObject.Mode.OUT);
				res.addPortDeclaration(s);
			}
		}
		if (!_matched) {
			if (Objects.equal(_direction, HDLVariableDeclaration.HDLDirection.INOUT)) {
				_matched = true;
				s.setMode(VhdlObject.Mode.INOUT);
				res.addPortDeclaration(s);
			}
		}
		if (!_matched) {
			if (Objects.equal(_direction, HDLVariableDeclaration.HDLDirection.INTERNAL)) {
				_matched = true;
				final HDLAnnotation _annotation = hvar.getAnnotation(HDLBuiltInAnnotationProvider.HDLBuiltInAnnotations.sharedVar);
				final boolean _tripleNotEquals_1 = (_annotation != null);
				if (_tripleNotEquals_1) {
					final String _name_1 = hvar.getName();
					final Expression _defaultValue_2 = s.getDefaultValue();
					final Variable sharedVar = new Variable(_name_1, varType, _defaultValue_2);
					sharedVar.setShared(true);
					final VariableDeclaration vd = new VariableDeclaration(sharedVar);
					res.addInternalSignalDeclaration(vd);
				} else {
					final SignalDeclaration sd = new SignalDeclaration(s);
					res.addInternalSignalDeclaration(sd);
				}
			}
		}
		if (!_matched) {
			if ((Objects.equal(obj.getDirection(), HDLVariableDeclaration.HDLDirection.HIDDEN)
					|| Objects.equal(obj.getDirection(), HDLVariableDeclaration.HDLDirection.CONSTANT))) {
				_matched = true;
				final ConstantDeclaration cd = new ConstantDeclaration(constant);
				final boolean _hasMeta = hvar.hasMeta(VHDLStatementExtension.EXPORT);
				if (_hasMeta) {
					res.addConstantDeclarationPkg(cd);
				} else {
					res.addConstantDeclaration(cd);
				}
			}
		}
		if (!_matched) {
			if (Objects.equal(_direction, HDLVariableDeclaration.HDLDirection.PARAMETER)) {
				_matched = true;
				res.addGenericDeclaration(constant);
			}
		}
	}

	protected VHDLContext _toVHDL(final HDLSwitchStatement obj, final int pid) {
		final VHDLContext context = new VHDLContext();
		final HDLExpression hCaseExp = obj.getCaseExp();
		Optional<BigInteger> width = Optional.<BigInteger> absent();
		final Optional<? extends HDLType> type = TypeExtension.typeOf(hCaseExp);
		if ((type.isPresent() && (type.get() instanceof HDLPrimitive))) {
			final HDLType _get = type.get();
			final HDLExpression _width = ((HDLPrimitive) _get).getWidth();
			final Optional<BigInteger> _valueOf = ConstantEvaluate.valueOf(_width, null);
			width = _valueOf;
			final boolean _isPresent = width.isPresent();
			final boolean _not = (!_isPresent);
			if (_not) {
				final HDLType _get_1 = type.get();
				throw new HDLCodeGenerationException(_get_1, "Switch cases need a constant width", "VHDL");
			}
		}
		final Expression caseExp = this.vee.toVHDL(hCaseExp);
		final Map<HDLSwitchCaseStatement, VHDLContext> ctxs = new LinkedHashMap<HDLSwitchCaseStatement, VHDLContext>();
		final Set<HDLRegisterConfig> configs = new LinkedHashSet<HDLRegisterConfig>();
		boolean hasUnclocked = false;
		final ArrayList<HDLSwitchCaseStatement> _cases = obj.getCases();
		for (final HDLSwitchCaseStatement cs : _cases) {
			{
				final VHDLContext vhdl = this.toVHDL(cs, pid);
				ctxs.put(cs, vhdl);
				final int _size = vhdl.unclockedStatements.size();
				final boolean _greaterThan = (_size > 0);
				if (_greaterThan) {
					hasUnclocked = true;
				}
				final Set<HDLRegisterConfig> _keySet = vhdl.clockedStatements.keySet();
				configs.addAll(_keySet);
			}
		}
		for (final HDLRegisterConfig hdlRegisterConfig : configs) {
			{
				final CaseStatement cs_1 = new CaseStatement(caseExp);
				final Set<Map.Entry<HDLSwitchCaseStatement, VHDLContext>> _entrySet = ctxs.entrySet();
				for (final Map.Entry<HDLSwitchCaseStatement, VHDLContext> e : _entrySet) {
					{
						final CaseStatement.Alternative alt = this.createAlternative(cs_1, e, width);
						final VHDLContext _value = e.getValue();
						final LinkedList<SequentialStatement> clockCase = _value.clockedStatements.get(hdlRegisterConfig);
						if ((clockCase != null)) {
							final List<SequentialStatement> _statements = alt.getStatements();
							_statements.addAll(clockCase);
						}
					}
				}
				context.addClockedStatement(hdlRegisterConfig, cs_1);
			}
		}
		if (hasUnclocked) {
			final CaseStatement cs_1 = new CaseStatement(caseExp);
			final Set<Map.Entry<HDLSwitchCaseStatement, VHDLContext>> _entrySet = ctxs.entrySet();
			for (final Map.Entry<HDLSwitchCaseStatement, VHDLContext> e : _entrySet) {
				{
					final CaseStatement.Alternative alt = this.createAlternative(cs_1, e, width);
					final VHDLContext _value = e.getValue();
					final LinkedList<SequentialStatement> _get_2 = _value.unclockedStatements.get(Integer.valueOf(pid));
					final boolean _tripleNotEquals = (_get_2 != null);
					if (_tripleNotEquals) {
						final List<SequentialStatement> _statements = alt.getStatements();
						final VHDLContext _value_1 = e.getValue();
						final LinkedList<SequentialStatement> _get_3 = _value_1.unclockedStatements.get(Integer.valueOf(pid));
						_statements.addAll(_get_3);
					}
				}
			}
			context.addUnclockedStatement(pid, cs_1, obj);
		}
		return this.attachComment(context, obj);
	}

	private CaseStatement.Alternative createAlternative(final CaseStatement cs, final Map.Entry<HDLSwitchCaseStatement, VHDLContext> e, final Optional<BigInteger> bits) {
		CaseStatement.Alternative alt = null;
		final HDLSwitchCaseStatement _key = e.getKey();
		final HDLExpression label = _key.getLabel();
		if ((label != null)) {
			final Optional<BigInteger> eval = ConstantEvaluate.valueOf(label, null);
			final boolean _isPresent = eval.isPresent();
			if (_isPresent) {
				final boolean _isPresent_1 = bits.isPresent();
				final boolean _not = (!_isPresent_1);
				if (_not)
					throw new IllegalArgumentException("The width needs to be known for primitive types!");
				final BigInteger _get = bits.get();
				final int _intValue = _get.intValue();
				final BigInteger _get_1 = eval.get();
				final Literal _binaryLiteral = VHDLUtils.toBinaryLiteral(_intValue, _get_1);
				final CaseStatement.Alternative _createAlternative = cs.createAlternative(_binaryLiteral);
				alt = _createAlternative;
			} else {
				final Expression _vHDL = this.vee.toVHDL(label);
				final CaseStatement.Alternative _createAlternative_1 = cs.createAlternative(_vHDL);
				alt = _createAlternative_1;
			}
		} else {
			final CaseStatement.Alternative _createAlternative_2 = cs.createAlternative(Choices.OTHERS);
			alt = _createAlternative_2;
		}
		return alt;
	}

	protected VHDLContext _toVHDL(final HDLSwitchCaseStatement obj, final int pid) {
		final VHDLContext res = new VHDLContext();
		final ArrayList<HDLStatement> _dos = obj.getDos();
		for (final HDLStatement stmnt : _dos) {
			final VHDLContext _vHDL = this.toVHDL(stmnt, pid);
			res.merge(_vHDL, false);
		}
		return this.attachComment(res, obj);
	}

	protected VHDLContext _toVHDL(final HDLAssignment obj, final int pid) {
		final VHDLContext context = new VHDLContext();
		SequentialStatement sa = null;
		final HDLReference ref = obj.getLeft();
		final HDLVariable hvar = ((HDLResolvedRef) ref).resolveVarForced("VHDL");
		final ArrayList<HDLExpression> dim = hvar.getDimensions();
		final Expression assTarget = this.vee.toVHDL(ref);
		final HDLExpression _right = obj.getRight();
		Expression value = this.vee.toVHDL(_right);
		if (((dim.size() != 0) && Objects.equal(ref.getClassType(), HDLClass.HDLVariableRef))) {
			final HDLVariableRef varRef = ((HDLVariableRef) ref);
			final ArrayList<HDLExpression> _array = varRef.getArray();
			for (final HDLExpression exp : _array) {
				dim.remove(0);
			}
			if (((dim.size() != 0) && (!Objects.equal(obj.getRight().getClassType(), HDLClass.HDLArrayInit)))) {
				final HDLAnnotation typeAnno = hvar.getAnnotation(HDLBuiltInAnnotationProvider.HDLBuiltInAnnotations.VHDLType);
				if ((typeAnno != null)) {
					final String _value = typeAnno.getValue();
					final UnresolvedType _unresolvedType = new UnresolvedType(_value);
					final HDLExpression _right_1 = obj.getRight();
					final Expression _vHDL = this.vee.toVHDL(_right_1);
					final TypeConversion _typeConversion = new TypeConversion(_unresolvedType, _vHDL);
					value = _typeConversion;
				} else {
					final HDLVariableDeclaration hvd = hvar.<HDLVariableDeclaration> getContainer(HDLVariableDeclaration.class);
					final boolean _isExternal = hvd.isExternal();
					final String _arrayRefName = VHDLStatementExtension.getArrayRefName(hvar, _isExternal);
					final UnresolvedType _unresolvedType_1 = new UnresolvedType(_arrayRefName);
					final HDLExpression _right_2 = obj.getRight();
					final Expression _vHDL_1 = this.vee.toVHDL(_right_2);
					final TypeConversion _typeConversion_1 = new TypeConversion(_unresolvedType_1, _vHDL_1);
					value = _typeConversion_1;
				}
			}
		}
		final HDLAnnotation _annotation = hvar.getAnnotation(HDLBuiltInAnnotationProvider.HDLBuiltInAnnotations.memory);
		final boolean _tripleNotEquals = (_annotation != null);
		if (_tripleNotEquals) {
			final VariableAssignment _variableAssignment = new VariableAssignment(((VariableAssignmentTarget) assTarget), value);
			sa = _variableAssignment;
		} else {
			final SignalAssignment _signalAssignment = new SignalAssignment(((SignalAssignmentTarget) assTarget), value);
			sa = _signalAssignment;
		}
		final HDLRegisterConfig config = hvar.getRegisterConfig();
		if ((config != null)) {
			context.addClockedStatement(config, sa);
		} else {
			context.addUnclockedStatement(pid, sa, obj);
		}
		return this.attachComment(context, obj);
	}

	protected VHDLContext _toVHDL(final HDLForLoop obj, final int pid) {
		final VHDLContext context = new VHDLContext();
		final ArrayList<HDLStatement> _dos = obj.getDos();
		for (final HDLStatement stmnt : _dos) {
			final VHDLContext _vHDL = this.toVHDL(stmnt, pid);
			context.merge(_vHDL, false);
		}
		final VHDLContext res = new VHDLContext();
		res.merge(context, true);
		final Set<Map.Entry<HDLRegisterConfig, LinkedList<SequentialStatement>>> _entrySet = context.clockedStatements.entrySet();
		for (final Map.Entry<HDLRegisterConfig, LinkedList<SequentialStatement>> e : _entrySet) {
			{
				final HDLVariable _param = obj.getParam();
				final String _name = _param.getName();
				final String _vHDLName = VHDLUtils.getVHDLName(_name);
				final ArrayList<HDLRange> _range = obj.getRange();
				final HDLRange _get = _range.get(0);
				final Range _vHDL_1 = this.vee.toVHDL(_get, Range.Direction.TO);
				final ForStatement fStmnt = new ForStatement(_vHDLName, _vHDL_1);
				final List<SequentialStatement> _statements = fStmnt.getStatements();
				final LinkedList<SequentialStatement> _value = e.getValue();
				_statements.addAll(_value);
				final HDLRegisterConfig _key = e.getKey();
				res.addClockedStatement(_key, fStmnt);
			}
		}
		final LinkedList<SequentialStatement> _get = context.unclockedStatements.get(Integer.valueOf(pid));
		final boolean _tripleNotEquals = (_get != null);
		if (_tripleNotEquals) {
			final HDLVariable _param = obj.getParam();
			final String _name = _param.getName();
			final String _vHDLName = VHDLUtils.getVHDLName(_name);
			final ArrayList<HDLRange> _range = obj.getRange();
			final HDLRange _get_1 = _range.get(0);
			final Range _vHDL_1 = this.vee.toVHDL(_get_1, Range.Direction.TO);
			final ForStatement fStmnt = new ForStatement(_vHDLName, _vHDL_1);
			final List<SequentialStatement> _statements = fStmnt.getStatements();
			final LinkedList<SequentialStatement> _get_2 = context.unclockedStatements.get(Integer.valueOf(pid));
			_statements.addAll(_get_2);
			res.addUnclockedStatement(pid, fStmnt, obj);
		}
		return this.attachComment(res, obj);
	}

	protected VHDLContext _toVHDL(final HDLIfStatement obj, final int pid) {
		final VHDLContext thenCtx = new VHDLContext();
		final ArrayList<HDLStatement> _thenDo = obj.getThenDo();
		for (final HDLStatement stmnt : _thenDo) {
			final VHDLContext _vHDL = this.toVHDL(stmnt, pid);
			thenCtx.merge(_vHDL, false);
		}
		final VHDLContext elseCtx = new VHDLContext();
		final ArrayList<HDLStatement> _elseDo = obj.getElseDo();
		for (final HDLStatement stmnt_1 : _elseDo) {
			final VHDLContext _vHDL_1 = this.toVHDL(stmnt_1, pid);
			elseCtx.merge(_vHDL_1, false);
		}
		final Set<HDLRegisterConfig> configs = new LinkedHashSet<HDLRegisterConfig>();
		final Set<HDLRegisterConfig> _keySet = thenCtx.clockedStatements.keySet();
		configs.addAll(_keySet);
		final Set<HDLRegisterConfig> _keySet_1 = elseCtx.clockedStatements.keySet();
		configs.addAll(_keySet_1);
		final VHDLContext res = new VHDLContext();
		res.merge(thenCtx, true);
		res.merge(elseCtx, true);
		final HDLExpression _ifExp = obj.getIfExp();
		final Expression ifExp = this.vee.toVHDL(_ifExp);
		for (final HDLRegisterConfig config : configs) {
			{
				final IfStatement ifs = new IfStatement(ifExp);
				final LinkedList<SequentialStatement> _get = thenCtx.clockedStatements.get(config);
				final boolean _tripleNotEquals = (_get != null);
				if (_tripleNotEquals) {
					final List<SequentialStatement> _statements = ifs.getStatements();
					final LinkedList<SequentialStatement> _get_1 = thenCtx.clockedStatements.get(config);
					_statements.addAll(_get_1);
				}
				final LinkedList<SequentialStatement> _get_2 = elseCtx.clockedStatements.get(config);
				final boolean _tripleNotEquals_1 = (_get_2 != null);
				if (_tripleNotEquals_1) {
					final List<SequentialStatement> _elseStatements = ifs.getElseStatements();
					final LinkedList<SequentialStatement> _get_3 = elseCtx.clockedStatements.get(config);
					_elseStatements.addAll(_get_3);
				}
				res.addClockedStatement(config, ifs);
			}
		}
		if (((thenCtx.unclockedStatements.size() != 0) || (elseCtx.unclockedStatements.size() != 0))) {
			final IfStatement ifs = new IfStatement(ifExp);
			final LinkedList<SequentialStatement> _get = thenCtx.unclockedStatements.get(Integer.valueOf(pid));
			final boolean _tripleNotEquals = (_get != null);
			if (_tripleNotEquals) {
				final List<SequentialStatement> _statements = ifs.getStatements();
				final LinkedList<SequentialStatement> _get_1 = thenCtx.unclockedStatements.get(Integer.valueOf(pid));
				_statements.addAll(_get_1);
			}
			final LinkedList<SequentialStatement> _get_2 = elseCtx.unclockedStatements.get(Integer.valueOf(pid));
			final boolean _tripleNotEquals_1 = (_get_2 != null);
			if (_tripleNotEquals_1) {
				final List<SequentialStatement> _elseStatements = ifs.getElseStatements();
				final LinkedList<SequentialStatement> _get_3 = elseCtx.unclockedStatements.get(Integer.valueOf(pid));
				_elseStatements.addAll(_get_3);
			}
			res.addUnclockedStatement(pid, ifs, obj);
		}
		return this.attachComment(res, obj);
	}

	protected VHDLContext _toVHDL(final HDLFunction obj, final int pid) {
		throw new IllegalArgumentException("Not supported");
	}

	public VHDLContext toVHDL(final IHDLObject obj, final int pid) {
		if (obj instanceof HDLBlock)
			return _toVHDL((HDLBlock) obj, pid);
		else if (obj instanceof HDLDirectGeneration)
			return _toVHDL((HDLDirectGeneration) obj, pid);
		else if (obj instanceof HDLEnumDeclaration)
			return _toVHDL((HDLEnumDeclaration) obj, pid);
		else if (obj instanceof HDLForLoop)
			return _toVHDL((HDLForLoop) obj, pid);
		else if (obj instanceof HDLFunction)
			return _toVHDL((HDLFunction) obj, pid);
		else if (obj instanceof HDLIfStatement)
			return _toVHDL((HDLIfStatement) obj, pid);
		else if (obj instanceof HDLInterfaceDeclaration)
			return _toVHDL((HDLInterfaceDeclaration) obj, pid);
		else if (obj instanceof HDLInterfaceInstantiation)
			return _toVHDL((HDLInterfaceInstantiation) obj, pid);
		else if (obj instanceof HDLSwitchCaseStatement)
			return _toVHDL((HDLSwitchCaseStatement) obj, pid);
		else if (obj instanceof HDLSwitchStatement)
			return _toVHDL((HDLSwitchStatement) obj, pid);
		else if (obj instanceof HDLVariableDeclaration)
			return _toVHDL((HDLVariableDeclaration) obj, pid);
		else if (obj instanceof HDLAssignment)
			return _toVHDL((HDLAssignment) obj, pid);
		else if (obj instanceof HDLExport)
			return _toVHDL((HDLExport) obj, pid);
		else if (obj instanceof HDLFunctionCall)
			return _toVHDL((HDLFunctionCall) obj, pid);
		else if (obj != null)
			return _toVHDL(obj, pid);
		else
			throw new IllegalArgumentException("Unhandled parameter types: " + Arrays.<Object> asList(obj, pid).toString());
	}
}
