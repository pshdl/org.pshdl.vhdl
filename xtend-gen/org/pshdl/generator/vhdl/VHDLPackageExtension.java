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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.xtend2.lib.StringConcatenation;
import org.eclipse.xtext.xbase.lib.CollectionExtensions;
import org.eclipse.xtext.xbase.lib.Conversions;
import org.eclipse.xtext.xbase.lib.Extension;
import org.eclipse.xtext.xbase.lib.InputOutput;
import org.eclipse.xtext.xbase.lib.IterableExtensions;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure1;
import org.pshdl.generator.vhdl.libraries.VHDLCastsLibrary;
import org.pshdl.generator.vhdl.libraries.VHDLShiftLibrary;
import org.pshdl.generator.vhdl.libraries.VHDLTypesLibrary;
import org.pshdl.model.HDLAnnotation;
import org.pshdl.model.HDLAssignment;
import org.pshdl.model.HDLClass;
import org.pshdl.model.HDLDeclaration;
import org.pshdl.model.HDLEnum;
import org.pshdl.model.HDLEnumDeclaration;
import org.pshdl.model.HDLEnumRef;
import org.pshdl.model.HDLExpression;
import org.pshdl.model.HDLPackage;
import org.pshdl.model.HDLReference;
import org.pshdl.model.HDLRegisterConfig;
import org.pshdl.model.HDLResolvedRef;
import org.pshdl.model.HDLStatement;
import org.pshdl.model.HDLUnit;
import org.pshdl.model.HDLUnresolvedFragment;
import org.pshdl.model.HDLVariable;
import org.pshdl.model.HDLVariableDeclaration;
import org.pshdl.model.HDLVariableRef;
import org.pshdl.model.IHDLObject;
import org.pshdl.model.extensions.FullNameExtension;
import org.pshdl.model.parser.SourceInfo;
import org.pshdl.model.types.builtIn.HDLBuiltInAnnotationProvider;
import org.pshdl.model.utils.HDLQualifiedName;
import org.pshdl.model.utils.ModificationSet;
import org.pshdl.model.utils.Refactoring;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.collect.Iterables;

import de.upb.hni.vmagic.AssociationElement;
import de.upb.hni.vmagic.VhdlElement;
import de.upb.hni.vmagic.VhdlFile;
import de.upb.hni.vmagic.builtin.NumericStd;
import de.upb.hni.vmagic.builtin.StdLogic1164;
import de.upb.hni.vmagic.concurrent.ConcurrentStatement;
import de.upb.hni.vmagic.concurrent.ProcessStatement;
import de.upb.hni.vmagic.declaration.BlockDeclarativeItem;
import de.upb.hni.vmagic.declaration.ConstantDeclaration;
import de.upb.hni.vmagic.declaration.DeclarativeItem;
import de.upb.hni.vmagic.declaration.DeclarativeItemMarker;
import de.upb.hni.vmagic.declaration.EntityDeclarativeItem;
import de.upb.hni.vmagic.declaration.PackageDeclarativeItem;
import de.upb.hni.vmagic.expression.Equals;
import de.upb.hni.vmagic.expression.Expression;
import de.upb.hni.vmagic.expression.FunctionCall;
import de.upb.hni.vmagic.libraryunit.Architecture;
import de.upb.hni.vmagic.libraryunit.Entity;
import de.upb.hni.vmagic.libraryunit.LibraryClause;
import de.upb.hni.vmagic.libraryunit.LibraryUnit;
import de.upb.hni.vmagic.libraryunit.PackageDeclaration;
import de.upb.hni.vmagic.libraryunit.UseClause;
import de.upb.hni.vmagic.literal.EnumerationLiteral;
import de.upb.hni.vmagic.object.Constant;
import de.upb.hni.vmagic.object.ConstantGroup;
import de.upb.hni.vmagic.object.Signal;
import de.upb.hni.vmagic.object.SignalGroup;
import de.upb.hni.vmagic.object.VhdlObjectProvider;
import de.upb.hni.vmagic.statement.IfStatement;
import de.upb.hni.vmagic.statement.SequentialStatement;
import de.upb.hni.vmagic.statement.WaitStatement;
import de.upb.hni.vmagic.type.Type;
import de.upb.hni.vmagic.type.UnresolvedType;
import de.upb.hni.vmagic.util.Comments;

@SuppressWarnings("all")
public class VHDLPackageExtension {
	@Extension
	private final VHDLExpressionExtension vee = new VHDLExpressionExtension();

	@Extension
	private final VHDLStatementExtension vse = new VHDLStatementExtension();

	public static VHDLPackageExtension INST = new VHDLPackageExtension();

	public List<LibraryUnit> toVHDL(final HDLUnit obj) {
		final List<LibraryUnit> res = new LinkedList<LibraryUnit>();
		final HDLQualifiedName entityName = FullNameExtension.fullNameOf(obj);
		final String _dashString = this.dashString(entityName);
		final Entity e = new Entity(_dashString);
		final VHDLContext unit = new VHDLContext();
		final HDLEnumRef[] hRefs = obj.<HDLEnumRef> getAllObjectsOf(HDLEnumRef.class, true);
		for (final HDLEnumRef hdlEnumRef : hRefs) {
			{
				final Optional<HDLEnum> resolveHEnum = hdlEnumRef.resolveHEnum();
				final HDLEnum _get = resolveHEnum.get();
				final HDLUnit enumContainer = _get.<HDLUnit> getContainer(HDLUnit.class);
				if (((enumContainer == null) || (!enumContainer.equals(hdlEnumRef.<HDLUnit> getContainer(HDLUnit.class))))) {
					final HDLEnum _get_1 = resolveHEnum.get();
					final HDLQualifiedName type = FullNameExtension.fullNameOf(_get_1);
					final String _segment = type.getSegment(0);
					final boolean _equals = _segment.equals("pshdl");
					final boolean _not = (!_equals);
					if (_not) {
						final String _packageName = this.getPackageName(type);
						final HDLQualifiedName _create = HDLQualifiedName.create("work", _packageName, "all");
						unit.addImport(_create);
					}
				}
			}
		}
		final HDLVariableRef[] vRefs = obj.<HDLVariableRef> getAllObjectsOf(HDLVariableRef.class, true);
		for (final HDLVariableRef variableRef : vRefs) {
			final HDLClass _classType = variableRef.getClassType();
			final boolean _notEquals = (!Objects.equal(_classType, HDLClass.HDLInterfaceRef));
			if (_notEquals) {
				final HDLVariable variable = variableRef.resolveVarForced("VHDL");
				final HDLUnit enumContainer = variable.<HDLUnit> getContainer(HDLUnit.class);
				if (((enumContainer == null) || (!enumContainer.equals(variableRef.<HDLUnit> getContainer(HDLUnit.class))))) {
					final HDLQualifiedName _fullNameOf = FullNameExtension.fullNameOf(variable);
					final HDLQualifiedName type = _fullNameOf.skipLast(1);
					if (((type.length > 0) && (!type.getSegment(0).equals("pshdl")))) {
						final String _packageName = this.getPackageName(type);
						final HDLQualifiedName _create = HDLQualifiedName.create("work", _packageName, "all");
						unit.addImport(_create);
					}
				}
			}
		}
		final ArrayList<HDLStatement> _inits = obj.getInits();
		for (final HDLStatement stmnt : _inits) {
			{
				final VHDLContext vhdl = this.vse.toVHDL(stmnt, VHDLContext.DEFAULT_CTX);
				if ((vhdl != null)) {
					unit.merge(vhdl, false);
				} else {
					InputOutput.<String> print(("Failed to translate: " + stmnt));
				}
			}
		}
		final ArrayList<HDLStatement> _statements = obj.getStatements();
		for (final HDLStatement stmnt_1 : _statements) {
			{
				final VHDLContext vhdl = this.vse.toVHDL(stmnt_1, VHDLContext.DEFAULT_CTX);
				if ((vhdl != null)) {
					unit.merge(vhdl, false);
				} else {
					InputOutput.<String> print(("Failed to translate: " + stmnt_1));
				}
			}
		}
		VHDLPackageExtension.addDefaultLibs(res, unit);
		final boolean _hasPkgDeclarations = unit.hasPkgDeclarations();
		if (_hasPkgDeclarations) {
			final String libName = this.getPackageName(entityName);
			final PackageDeclaration pd = new PackageDeclaration(libName);
			final List<PackageDeclarativeItem> _declarations = pd.getDeclarations();
			_declarations.addAll(((List) unit.externalTypes));
			final List<PackageDeclarativeItem> _declarations_1 = pd.getDeclarations();
			_declarations_1.addAll(unit.constantsPkg);
			res.add(pd);
			final StringConcatenation _builder = new StringConcatenation();
			_builder.append("work.");
			_builder.append(libName, "");
			_builder.append(".all");
			final UseClause _useClause = new UseClause(_builder.toString());
			res.add(_useClause);
			VHDLPackageExtension.addDefaultLibs(res, unit);
		}
		for (final Signal sig : unit.ports) {
			{
				final List<String> comments = Comments.getComments(sig);
				final SignalGroup sg = new SignalGroup(sig);
				Comments.setComments(sg, comments);
				final List<VhdlObjectProvider<Signal>> _port = e.getPort();
				_port.add(sg);
			}
		}
		for (final Constant sig_1 : unit.generics) {
			{
				final List<String> comments = Comments.getComments(sig_1);
				final ConstantGroup sg = new ConstantGroup(sig_1);
				Comments.setComments(sg, comments);
				final List<VhdlObjectProvider<Constant>> _generic = e.getGeneric();
				_generic.add(sg);
			}
		}
		final List<EntityDeclarativeItem> _declarations_2 = e.getDeclarations();
		_declarations_2.addAll(((List) unit.internalTypesConstants));
		res.add(e);
		final Architecture a = new Architecture("pshdlGenerated", e);
		this.attachComments(e, obj, true, false);
		this.attachComments(a, obj, false, true);
		final List<BlockDeclarativeItem> _declarations_3 = a.getDeclarations();
		final Collection<DeclarativeItem> _values = unit.components.values();
		_declarations_3.addAll(((Collection) _values));
		final List<BlockDeclarativeItem> _declarations_4 = a.getDeclarations();
		_declarations_4.addAll(((List) unit.internals));
		final List<ConcurrentStatement> _statements_1 = a.getStatements();
		_statements_1.addAll(unit.concurrentStatements);
		final Set<Map.Entry<Integer, LinkedList<SequentialStatement>>> _entrySet = unit.unclockedStatements.entrySet();
		for (final Map.Entry<Integer, LinkedList<SequentialStatement>> uc : _entrySet) {
			{
				final ProcessStatement ps = new ProcessStatement();
				final List<Signal> _sensitivityList = ps.getSensitivityList();
				final Integer _key = uc.getKey();
				final Collection<? extends Signal> _createSensitivyList = this.createSensitivyList(unit, (_key).intValue());
				_sensitivityList.addAll(_createSensitivyList);
				final List<SequentialStatement> _statements_2 = ps.getStatements();
				final LinkedList<SequentialStatement> _value = uc.getValue();
				_statements_2.addAll(_value);
				final List<Signal> _sensitivityList_1 = ps.getSensitivityList();
				final boolean _isEmpty = _sensitivityList_1.isEmpty();
				if (_isEmpty) {
					final WaitSeacher ssv = new WaitSeacher();
					final List<SequentialStatement> _statements_3 = ps.getStatements();
					final Procedure1<SequentialStatement> _function = new Procedure1<SequentialStatement>() {
						@Override
						public void apply(final SequentialStatement it) {
							ssv.visit(it);
						}
					};
					IterableExtensions.<SequentialStatement> forEach(_statements_3, _function);
					if ((!ssv.hasWait)) {
						final List<SequentialStatement> _statements_4 = ps.getStatements();
						final WaitStatement _waitStatement = new WaitStatement();
						_statements_4.add(_waitStatement);
					}
				}
				final List<ConcurrentStatement> _statements_5 = a.getStatements();
				_statements_5.add(ps);
			}
		}
		final Set<Map.Entry<HDLRegisterConfig, LinkedList<SequentialStatement>>> _entrySet_1 = unit.clockedStatements.entrySet();
		for (final Map.Entry<HDLRegisterConfig, LinkedList<SequentialStatement>> pc : _entrySet_1) {
			{
				final ProcessStatement ps = new ProcessStatement();
				final List<SequentialStatement> _statements_2 = ps.getStatements();
				final HDLRegisterConfig _key = pc.getKey();
				final LinkedList<SequentialStatement> _value = pc.getValue();
				final SequentialStatement _createIfStatement = this.createIfStatement(obj, ps, _key, _value, unit);
				_statements_2.add(_createIfStatement);
				final List<ConcurrentStatement> _statements_3 = a.getStatements();
				_statements_3.add(ps);
			}
		}
		res.add(a);
		return res;
	}

	public void attachComments(final VhdlElement e, final IHDLObject obj, final boolean doc, final boolean normal) {
		final SourceInfo srcInfo = obj.<SourceInfo> getMeta(SourceInfo.INFO);
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
			if ((doc && normal)) {
				final Iterable<String> _plus = Iterables.<String> concat(newComments, docComments);
				Comments.setComments(e, ((String[]) Conversions.unwrapArray(_plus, String.class)));
			} else {
				if (doc) {
					Comments.setComments(e, docComments);
				} else {
					if (normal) {
						Comments.setComments(e, newComments);
					}
				}
			}
		}
	}

	public String getPackageName(final HDLQualifiedName entityName) {
		final StringConcatenation _builder = new StringConcatenation();
		final String _dashString = this.dashString(entityName);
		_builder.append(_dashString, "");
		_builder.append("Pkg");
		return _builder.toString();
	}

	public String dashString(final HDLQualifiedName name) {
		final char _charAt = "_".charAt(0);
		return name.toString(_charAt);
	}

	public HDLQualifiedName getPackageNameRef(final HDLQualifiedName entityName) {
		final String _segment = entityName.getSegment(0);
		final boolean _equals = _segment.equals("VHDL");
		if (_equals)
			return entityName.skipFirst(1);
		final StringConcatenation _builder = new StringConcatenation();
		final String _dashString = this.dashString(entityName);
		_builder.append(_dashString, "");
		_builder.append("Pkg");
		return HDLQualifiedName.create("work", _builder.toString());
	}

	public HDLQualifiedName getNameRef(final HDLQualifiedName entityName) {
		final String _segment = entityName.getSegment(0);
		final boolean _equals = _segment.equals("VHDL");
		if (_equals)
			return entityName.skipFirst(1);
		final String _dashString = this.dashString(entityName);
		return HDLQualifiedName.create("work", _dashString);
	}

	private static void addDefaultLibs(final List<LibraryUnit> res, final VHDLContext unit) {
		final Set<String> usedLibs = VHDLPackageExtension.staticImports(res);
		for (final HDLQualifiedName i : unit.imports) {
			{
				final String lib = i.getSegment(0);
				final boolean _contains = usedLibs.contains(lib);
				final boolean _not = (!_contains);
				if (_not) {
					final LibraryClause _libraryClause = new LibraryClause(lib);
					res.add(_libraryClause);
					usedLibs.add(lib);
				}
				final HDLQualifiedName _append = i.append("all");
				final String _string = _append.toString();
				final UseClause _useClause = new UseClause(_string);
				res.add(_useClause);
			}
		}
	}

	public static Set<String> staticImports(final List<LibraryUnit> res) {
		final LibraryClause _libraryClause = new LibraryClause("ieee");
		res.add(_libraryClause);
		res.add(StdLogic1164.USE_CLAUSE);
		res.add(NumericStd.USE_CLAUSE);
		res.add(VHDLCastsLibrary.USE_CLAUSE);
		res.add(VHDLShiftLibrary.USE_CLAUSE);
		res.add(VHDLTypesLibrary.USE_CLAUSE);
		final Set<String> usedLibs = new LinkedHashSet<String>();
		usedLibs.add("ieee");
		usedLibs.add("work");
		return usedLibs;
	}

	private static EnumSet<HDLVariableDeclaration.HDLDirection> notSensitive = EnumSet.<HDLVariableDeclaration.HDLDirection> of(HDLVariableDeclaration.HDLDirection.HIDDEN,
			HDLVariableDeclaration.HDLDirection.PARAMETER, HDLVariableDeclaration.HDLDirection.CONSTANT);

	private Collection<? extends Signal> createSensitivyList(final VHDLContext ctx, final int pid) {
		final boolean _containsKey = ctx.noSensitivity.containsKey(Integer.valueOf(pid));
		if (_containsKey)
			return Collections.<Signal> emptyList();
		final List<Signal> sensitivity = new LinkedList<Signal>();
		final Set<String> vars = new TreeSet<String>();
		final LinkedList<HDLStatement> _get = ctx.sensitiveStatements.get(Integer.valueOf(pid));
		for (final HDLStatement stmnt : _get) {
			{
				final HDLVariableRef[] refs = stmnt.<HDLVariableRef> getAllObjectsOf(HDLVariableRef.class, true);
				for (final HDLVariableRef ref : refs) {
					{
						final HDLVariable hvar = ref.resolveVarForced("VHDL");
						final HDLAnnotation _annotation = hvar.getAnnotation(HDLBuiltInAnnotationProvider.HDLBuiltInAnnotations.memory);
						final boolean _tripleEquals = (_annotation == null);
						if (_tripleEquals) {
							final IHDLObject container = hvar.getContainer();
							if ((container instanceof HDLVariableDeclaration)) {
								final HDLVariableDeclaration hdv = ((HDLVariableDeclaration) container);
								final HDLVariableDeclaration.HDLDirection _direction = hdv.getDirection();
								final boolean _contains = VHDLPackageExtension.notSensitive.contains(_direction);
								final boolean _not = (!_contains);
								if (_not) {
									final IHDLObject _container = ref.getContainer();
									if ((_container instanceof HDLAssignment)) {
										final IHDLObject _container_1 = ref.getContainer();
										final HDLAssignment hAss = ((HDLAssignment) _container_1);
										final HDLReference _left = hAss.getLeft();
										final HDLVariable _resolveVar = this.resolveVar(_left);
										final HDLRegisterConfig _registerConfig = _resolveVar.getRegisterConfig();
										final boolean _tripleNotEquals = (_registerConfig != null);
										if (_tripleNotEquals) {
										} else {
											final HDLReference _left_1 = hAss.getLeft();
											final boolean _notEquals = (!Objects.equal(_left_1, ref));
											if (_notEquals) {
												final String _vHDLName = this.vee.getVHDLName(ref);
												vars.add(_vHDLName);
											}
										}
									} else {
										final String _vHDLName_1 = this.vee.getVHDLName(ref);
										vars.add(_vHDLName_1);
									}
								}
							}
						}
					}
				}
			}
		}
		for (final String string : vars) {
			final Signal _signal = new Signal(string, UnresolvedType.NO_NAME);
			sensitivity.add(_signal);
		}
		return sensitivity;
	}

	public HDLVariable resolveVar(final HDLReference reference) {
		if ((reference instanceof HDLUnresolvedFragment))
			throw new RuntimeException("Can not use unresolved fragments");
		final Optional<HDLVariable> _resolveVar = ((HDLResolvedRef) reference).resolveVar();
		return _resolveVar.get();
	}

	private SequentialStatement createIfStatement(final HDLUnit hUnit, final ProcessStatement ps, final HDLRegisterConfig config, final LinkedList<SequentialStatement> value,
			final VHDLContext unit) {
		final HDLRegisterConfig key = config.normalize();
		final HDLExpression _clk = key.getClk();
		final Expression _vHDL = this.vee.toVHDL(_clk);
		final Signal clk = ((Signal) _vHDL);
		final HDLExpression _rst = key.getRst();
		final Expression _vHDL_1 = this.vee.toVHDL(_rst);
		final Signal rst = ((Signal) _vHDL_1);
		final List<Signal> _sensitivityList = ps.getSensitivityList();
		_sensitivityList.add(clk);
		EnumerationLiteral activeRst = null;
		final HDLRegisterConfig.HDLRegResetActiveType _resetType = key.getResetType();
		final boolean _tripleEquals = (_resetType == HDLRegisterConfig.HDLRegResetActiveType.HIGH);
		if (_tripleEquals) {
			activeRst = StdLogic1164.STD_LOGIC_1;
		} else {
			activeRst = StdLogic1164.STD_LOGIC_0;
		}
		final Equals _equals = new Equals(rst, activeRst);
		final IfStatement rstIfStmnt = new IfStatement(_equals);
		final LinkedList<SequentialStatement> resets = unit.resetStatements.get(key);
		if ((resets != null)) {
			final List<SequentialStatement> _statements = rstIfStmnt.getStatements();
			_statements.addAll(resets);
		}
		FunctionCall edge = null;
		final HDLRegisterConfig.HDLRegClockType _clockType = key.getClockType();
		final boolean _tripleEquals_1 = (_clockType == HDLRegisterConfig.HDLRegClockType.RISING);
		if (_tripleEquals_1) {
			final FunctionCall _functionCall = new FunctionCall(StdLogic1164.RISING_EDGE);
			edge = _functionCall;
		} else {
			final FunctionCall _functionCall_1 = new FunctionCall(StdLogic1164.FALLING_EDGE);
			edge = _functionCall_1;
		}
		final List<AssociationElement> _parameters = edge.getParameters();
		final AssociationElement _associationElement = new AssociationElement(clk);
		_parameters.add(_associationElement);
		final HDLRegisterConfig.HDLRegSyncType _syncType = key.getSyncType();
		final boolean _tripleEquals_2 = (_syncType == HDLRegisterConfig.HDLRegSyncType.ASYNC);
		if (_tripleEquals_2) {
			final List<Signal> _sensitivityList_1 = ps.getSensitivityList();
			_sensitivityList_1.add(rst);
			final IfStatement.ElsifPart elsifPart = rstIfStmnt.createElsifPart(edge);
			final List<SequentialStatement> _statements_1 = elsifPart.getStatements();
			_statements_1.addAll(value);
			return rstIfStmnt;
		}
		final IfStatement clkIf = new IfStatement(edge);
		final List<SequentialStatement> _statements_2 = clkIf.getStatements();
		_statements_2.add(rstIfStmnt);
		final List<SequentialStatement> _elseStatements = rstIfStmnt.getElseStatements();
		_elseStatements.addAll(value);
		return clkIf;
	}

	public VhdlFile toVHDL(final HDLPackage obj) {
		final VhdlFile res = new VhdlFile();
		PackageDeclaration pd = null;
		final ArrayList<HDLDeclaration> _declarations = obj.getDeclarations();
		for (final HDLDeclaration decl : _declarations) {
			{
				final HDLClass _classType = decl.getClassType();
				final boolean _equals = Objects.equal(_classType, HDLClass.HDLVariableDeclaration);
				if (_equals) {
					final HDLVariableDeclaration hvd = ((HDLVariableDeclaration) decl);
					if ((pd == null)) {
						final List<LibraryUnit> _elements = res.getElements();
						VHDLPackageExtension.staticImports(_elements);
						final String _pkg = obj.getPkg();
						final HDLQualifiedName _hDLQualifiedName = new HDLQualifiedName(_pkg);
						final String _packageName = this.getPackageName(_hDLQualifiedName);
						final PackageDeclaration _packageDeclaration = new PackageDeclaration(_packageName);
						pd = _packageDeclaration;
						final List<LibraryUnit> _elements_1 = res.getElements();
						_elements_1.add(pd);
					}
					final VHDLContext vhdl = this.vse.toVHDL(hvd, VHDLContext.DEFAULT_CTX);
					ConstantDeclaration _xifexpression = null;
					final boolean _isEmpty = vhdl.constants.isEmpty();
					if (_isEmpty) {
						_xifexpression = null;
					} else {
						_xifexpression = vhdl.constants.getFirst();
					}
					ConstantDeclaration first = _xifexpression;
					if ((first == null)) {
						final ConstantDeclaration _first = vhdl.constantsPkg.getFirst();
						first = _first;
						if ((first == null))
							throw new IllegalArgumentException("Expected constant declaration but found none!");
					}
					final List<PackageDeclarativeItem> _declarations_1 = pd.getDeclarations();
					_declarations_1.add(first);
				}
				final HDLClass _classType_1 = decl.getClassType();
				final boolean _equals_1 = Objects.equal(_classType_1, HDLClass.HDLEnumDeclaration);
				if (_equals_1) {
					final HDLEnumDeclaration hvd_1 = ((HDLEnumDeclaration) decl);
					final HDLEnum _hEnum = hvd_1.getHEnum();
					final HDLQualifiedName _fullNameOf = FullNameExtension.fullNameOf(_hEnum);
					final String _packageName_1 = this.getPackageName(_fullNameOf);
					final PackageDeclaration enumPd = new PackageDeclaration(_packageName_1);
					final List<LibraryUnit> _elements_2 = res.getElements();
					_elements_2.add(enumPd);
					final VHDLContext vhdl_1 = this.vse.toVHDL(hvd_1, VHDLContext.DEFAULT_CTX);
					final DeclarativeItemMarker _first_1 = vhdl_1.internalTypes.getFirst();
					final Type first_1 = ((Type) _first_1);
					if ((first_1 == null))
						throw new IllegalArgumentException("Expected enum type declaration but found none!");
					final List<PackageDeclarativeItem> _declarations_2 = enumPd.getDeclarations();
					_declarations_2.add(first_1);
				}
			}
		}
		final ArrayList<HDLUnit> _units = obj.getUnits();
		for (final HDLUnit unit : _units) {
			{
				final ModificationSet ms = new ModificationSet();
				final HDLVariableDeclaration[] hvds = unit.<HDLVariableDeclaration> getAllObjectsOf(HDLVariableDeclaration.class, true);
				for (final HDLVariableDeclaration hvd : hvds) {
					final ArrayList<HDLVariable> _variables = hvd.getVariables();
					for (final HDLVariable hvar : _variables) {
						{
							final HDLVariableRef[] refs = hvar.<HDLVariableRef> getAllObjectsOf(HDLVariableRef.class, true);
							for (final HDLVariableRef ref : refs) {
								{
									final Optional<HDLVariable> resolvedRef = ref.resolveVar();
									final boolean _isPresent = resolvedRef.isPresent();
									if (_isPresent) {
										final HDLVariable _get = resolvedRef.get();
										_get.setMeta(VHDLStatementExtension.EXPORT);
									}
								}
							}
							final String origName = hvar.getName();
							final String name = VHDLUtils.getVHDLName(origName);
							final boolean _notEquals = (!Objects.equal(name, origName));
							if (_notEquals) {
								final HDLQualifiedName _asRef = hvar.asRef();
								final HDLQualifiedName _skipLast = _asRef.skipLast(1);
								final HDLQualifiedName _append = _skipLast.append(name);
								Refactoring.<HDLUnit> renameVariable(hvar, _append, unit, ms);
							}
						}
					}
				}
				final HDLUnit newUnit = ms.<HDLUnit> apply(unit);
				final List<LibraryUnit> _elements = res.getElements();
				final List<LibraryUnit> _vHDL = this.toVHDL(newUnit);
				_elements.addAll(_vHDL);
			}
		}
		return res;
	}
}
