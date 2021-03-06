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
import com.google.common.collect.Iterables;
import de.upb.hni.vmagic.AssociationElement;
import de.upb.hni.vmagic.VhdlElement;
import de.upb.hni.vmagic.VhdlFile;
import de.upb.hni.vmagic.builtin.NumericStd;
import de.upb.hni.vmagic.builtin.StdLogic1164;
import de.upb.hni.vmagic.concurrent.ProcessStatement;
import de.upb.hni.vmagic.declaration.ConstantDeclaration;
import de.upb.hni.vmagic.declaration.DeclarativeItem;
import de.upb.hni.vmagic.declaration.DeclarativeItemMarker;
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
import de.upb.hni.vmagic.statement.IfStatement;
import de.upb.hni.vmagic.statement.SequentialStatement;
import de.upb.hni.vmagic.statement.WaitStatement;
import de.upb.hni.vmagic.type.Type;
import de.upb.hni.vmagic.type.UnresolvedType;
import de.upb.hni.vmagic.util.Comments;
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
import java.util.function.Consumer;
import org.eclipse.xtend2.lib.StringConcatenation;
import org.eclipse.xtext.xbase.lib.CollectionExtensions;
import org.eclipse.xtext.xbase.lib.Conversions;
import org.eclipse.xtext.xbase.lib.Extension;
import org.eclipse.xtext.xbase.lib.InputOutput;
import org.pshdl.generator.vhdl.VHDLContext;
import org.pshdl.generator.vhdl.VHDLExpressionExtension;
import org.pshdl.generator.vhdl.VHDLStatementExtension;
import org.pshdl.generator.vhdl.VHDLUtils;
import org.pshdl.generator.vhdl.WaitSeacher;
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

@SuppressWarnings("all")
public class VHDLPackageExtension {
  @Extension
  private VHDLExpressionExtension vee = new VHDLExpressionExtension();
  
  @Extension
  private VHDLStatementExtension vse = new VHDLStatementExtension();
  
  public static VHDLPackageExtension INST = new VHDLPackageExtension();
  
  public List<LibraryUnit> toVHDL(final HDLUnit obj) {
    final List<LibraryUnit> res = new LinkedList<LibraryUnit>();
    final HDLQualifiedName entityName = FullNameExtension.fullNameOf(obj);
    String _dashString = this.dashString(entityName);
    final Entity e = new Entity(_dashString);
    final VHDLContext unit = new VHDLContext();
    final HDLEnumRef[] hRefs = obj.<HDLEnumRef>getAllObjectsOf(HDLEnumRef.class, true);
    for (final HDLEnumRef hdlEnumRef : hRefs) {
      {
        final Optional<HDLEnum> resolveHEnum = hdlEnumRef.resolveHEnum();
        final HDLUnit enumContainer = resolveHEnum.get().<HDLUnit>getContainer(HDLUnit.class);
        if (((enumContainer == null) || (!enumContainer.equals(hdlEnumRef.<HDLUnit>getContainer(HDLUnit.class))))) {
          final HDLQualifiedName type = FullNameExtension.fullNameOf(resolveHEnum.get());
          boolean _equals = type.getSegment(0).equals("pshdl");
          boolean _not = (!_equals);
          if (_not) {
            unit.addImport(HDLQualifiedName.create("work", this.getPackageName(type), "all"));
          }
        }
      }
    }
    final HDLVariableRef[] vRefs = obj.<HDLVariableRef>getAllObjectsOf(HDLVariableRef.class, true);
    for (final HDLVariableRef variableRef : vRefs) {
      HDLClass _classType = variableRef.getClassType();
      boolean _notEquals = (!Objects.equal(_classType, HDLClass.HDLInterfaceRef));
      if (_notEquals) {
        final HDLVariable variable = variableRef.resolveVarForced("VHDL");
        final HDLUnit enumContainer = variable.<HDLUnit>getContainer(HDLUnit.class);
        if (((enumContainer == null) || (!enumContainer.equals(variableRef.<HDLUnit>getContainer(HDLUnit.class))))) {
          final HDLQualifiedName type = FullNameExtension.fullNameOf(variable).skipLast(1);
          if (((type.length > 0) && (!type.getSegment(0).equals("pshdl")))) {
            unit.addImport(HDLQualifiedName.create("work", this.getPackageName(type), "all"));
          }
        }
      }
    }
    ArrayList<HDLStatement> _inits = obj.getInits();
    for (final HDLStatement stmnt : _inits) {
      {
        final VHDLContext vhdl = this.vse.toVHDL(stmnt, VHDLContext.DEFAULT_CTX);
        if ((vhdl != null)) {
          unit.merge(vhdl, false);
        } else {
          InputOutput.<String>print(("Failed to translate: " + stmnt));
        }
      }
    }
    ArrayList<HDLStatement> _statements = obj.getStatements();
    for (final HDLStatement stmnt_1 : _statements) {
      {
        final VHDLContext vhdl = this.vse.toVHDL(stmnt_1, VHDLContext.DEFAULT_CTX);
        if ((vhdl != null)) {
          unit.merge(vhdl, false);
        } else {
          InputOutput.<String>print(("Failed to translate: " + stmnt_1));
        }
      }
    }
    VHDLPackageExtension.addDefaultLibs(res, unit);
    boolean _hasPkgDeclarations = unit.hasPkgDeclarations();
    if (_hasPkgDeclarations) {
      final String libName = this.getPackageName(entityName);
      final PackageDeclaration pd = new PackageDeclaration(libName);
      pd.getDeclarations().addAll(((List) unit.externalTypes));
      pd.getDeclarations().addAll(unit.constantsPkg);
      res.add(pd);
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("work.");
      _builder.append(libName);
      _builder.append(".all");
      UseClause _useClause = new UseClause(_builder.toString());
      res.add(_useClause);
      VHDLPackageExtension.addDefaultLibs(res, unit);
    }
    for (final Signal sig : unit.ports) {
      {
        final List<String> comments = Comments.getComments(sig);
        final SignalGroup sg = new SignalGroup(sig);
        Comments.setComments(sg, comments);
        e.getPort().add(sg);
      }
    }
    for (final Constant sig_1 : unit.generics) {
      {
        final List<String> comments = Comments.getComments(sig_1);
        final ConstantGroup sg = new ConstantGroup(sig_1);
        Comments.setComments(sg, comments);
        e.getGeneric().add(sg);
      }
    }
    e.getDeclarations().addAll(((List) unit.internalTypesConstants));
    res.add(e);
    final Architecture a = new Architecture("pshdlGenerated", e);
    this.attachComments(e, obj, true, false);
    this.attachComments(a, obj, false, true);
    Collection<DeclarativeItem> _values = unit.components.values();
    a.getDeclarations().addAll(((Collection) _values));
    a.getDeclarations().addAll(((List) unit.internals));
    a.getStatements().addAll(unit.concurrentStatements);
    Set<Map.Entry<Integer, LinkedList<SequentialStatement>>> _entrySet = unit.unclockedStatements.entrySet();
    for (final Map.Entry<Integer, LinkedList<SequentialStatement>> uc : _entrySet) {
      {
        final ProcessStatement ps = new ProcessStatement();
        ps.getSensitivityList().addAll(this.createSensitivyList(unit, (uc.getKey()).intValue()));
        ps.getStatements().addAll(uc.getValue());
        boolean _isEmpty = ps.getSensitivityList().isEmpty();
        if (_isEmpty) {
          final WaitSeacher ssv = new WaitSeacher();
          final Consumer<SequentialStatement> _function = (SequentialStatement it) -> {
            ssv.visit(it);
          };
          ps.getStatements().forEach(_function);
          if ((!ssv.hasWait)) {
            List<SequentialStatement> _statements_1 = ps.getStatements();
            WaitStatement _waitStatement = new WaitStatement();
            _statements_1.add(_waitStatement);
          }
        }
        a.getStatements().add(ps);
      }
    }
    Set<Map.Entry<HDLRegisterConfig, LinkedList<SequentialStatement>>> _entrySet_1 = unit.clockedStatements.entrySet();
    for (final Map.Entry<HDLRegisterConfig, LinkedList<SequentialStatement>> pc : _entrySet_1) {
      {
        final ProcessStatement ps = new ProcessStatement();
        ps.getStatements().add(this.createIfStatement(obj, ps, pc.getKey(), pc.getValue(), unit));
        a.getStatements().add(ps);
      }
    }
    res.add(a);
    return res;
  }
  
  public void attachComments(final VhdlElement e, final IHDLObject obj, final boolean doc, final boolean normal) {
    final SourceInfo srcInfo = obj.<SourceInfo>getMeta(SourceInfo.INFO);
    if ((srcInfo != null)) {
      final ArrayList<String> newComments = new ArrayList<String>();
      final ArrayList<String> docComments = new ArrayList<String>();
      for (final String comment : srcInfo.comments) {
        boolean _startsWith = comment.startsWith("//");
        if (_startsWith) {
          int _length = comment.length();
          int _minus = (_length - 1);
          final String newComment = comment.substring(2, _minus);
          boolean _startsWith_1 = newComment.startsWith("/");
          if (_startsWith_1) {
            boolean _startsWith_2 = newComment.startsWith("/<");
            if (_startsWith_2) {
              docComments.add(newComment.substring(2));
            } else {
              docComments.add(newComment.substring(1));
            }
          } else {
            newComments.add(newComment);
          }
        } else {
          int _length_1 = comment.length();
          int _minus_1 = (_length_1 - 2);
          final String newComment_1 = comment.substring(2, _minus_1);
          boolean _startsWith_3 = newComment_1.startsWith("*");
          if (_startsWith_3) {
            boolean _startsWith_4 = newComment_1.startsWith("*<");
            if (_startsWith_4) {
              CollectionExtensions.<String>addAll(docComments, newComment_1.substring(2).split("\n"));
            } else {
              CollectionExtensions.<String>addAll(docComments, newComment_1.substring(1).split("\n"));
            }
          } else {
            CollectionExtensions.<String>addAll(newComments, newComment_1.split("\n"));
          }
        }
      }
      if ((doc && normal)) {
        Iterable<String> _plus = Iterables.<String>concat(newComments, docComments);
        Comments.setComments(e, ((String[])Conversions.unwrapArray(_plus, String.class)));
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
    StringConcatenation _builder = new StringConcatenation();
    String _dashString = this.dashString(entityName);
    _builder.append(_dashString);
    _builder.append("Pkg");
    return _builder.toString();
  }
  
  public String dashString(final HDLQualifiedName name) {
    return name.toString("_".charAt(0));
  }
  
  public HDLQualifiedName getPackageNameRef(final HDLQualifiedName entityName) {
    boolean _equals = entityName.getSegment(0).equals("VHDL");
    if (_equals) {
      return entityName.skipFirst(1);
    }
    StringConcatenation _builder = new StringConcatenation();
    String _dashString = this.dashString(entityName);
    _builder.append(_dashString);
    _builder.append("Pkg");
    return HDLQualifiedName.create("work", _builder.toString());
  }
  
  public HDLQualifiedName getNameRef(final HDLQualifiedName entityName) {
    boolean _equals = entityName.getSegment(0).equals("VHDL");
    if (_equals) {
      return entityName.skipFirst(1);
    }
    return HDLQualifiedName.create("work", this.dashString(entityName));
  }
  
  private static void addDefaultLibs(final List<LibraryUnit> res, final VHDLContext unit) {
    final Set<String> usedLibs = VHDLPackageExtension.staticImports(res);
    for (final HDLQualifiedName i : unit.imports) {
      {
        final String lib = i.getSegment(0);
        boolean _contains = usedLibs.contains(lib);
        boolean _not = (!_contains);
        if (_not) {
          LibraryClause _libraryClause = new LibraryClause(lib);
          res.add(_libraryClause);
          usedLibs.add(lib);
        }
        String _string = i.append("all").toString();
        UseClause _useClause = new UseClause(_string);
        res.add(_useClause);
      }
    }
  }
  
  public static Set<String> staticImports(final List<LibraryUnit> res) {
    LibraryClause _libraryClause = new LibraryClause("ieee");
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
  
  private static EnumSet<HDLVariableDeclaration.HDLDirection> notSensitive = EnumSet.<HDLVariableDeclaration.HDLDirection>of(HDLVariableDeclaration.HDLDirection.HIDDEN, HDLVariableDeclaration.HDLDirection.PARAMETER, 
    HDLVariableDeclaration.HDLDirection.CONSTANT);
  
  private Collection<? extends Signal> createSensitivyList(final VHDLContext ctx, final int pid) {
    boolean _containsKey = ctx.noSensitivity.containsKey(Integer.valueOf(pid));
    if (_containsKey) {
      return Collections.<Signal>emptyList();
    }
    final List<Signal> sensitivity = new LinkedList<Signal>();
    final Set<String> vars = new TreeSet<String>();
    LinkedList<HDLStatement> _get = ctx.sensitiveStatements.get(Integer.valueOf(pid));
    for (final HDLStatement stmnt : _get) {
      {
        final HDLVariableRef[] refs = stmnt.<HDLVariableRef>getAllObjectsOf(HDLVariableRef.class, true);
        for (final HDLVariableRef ref : refs) {
          {
            final HDLVariable hvar = ref.resolveVarForced("VHDL");
            HDLAnnotation _annotation = hvar.getAnnotation(HDLBuiltInAnnotationProvider.HDLBuiltInAnnotations.memory);
            boolean _tripleEquals = (_annotation == null);
            if (_tripleEquals) {
              final IHDLObject container = hvar.getContainer();
              if ((container instanceof HDLVariableDeclaration)) {
                final HDLVariableDeclaration hdv = ((HDLVariableDeclaration) container);
                boolean _contains = VHDLPackageExtension.notSensitive.contains(hdv.getDirection());
                boolean _not = (!_contains);
                if (_not) {
                  IHDLObject _container = ref.getContainer();
                  if ((_container instanceof HDLAssignment)) {
                    IHDLObject _container_1 = ref.getContainer();
                    final HDLAssignment hAss = ((HDLAssignment) _container_1);
                    HDLRegisterConfig _registerConfig = this.resolveVar(hAss.getLeft()).getRegisterConfig();
                    boolean _tripleNotEquals = (_registerConfig != null);
                    if (_tripleNotEquals) {
                    } else {
                      HDLReference _left = hAss.getLeft();
                      boolean _notEquals = (!Objects.equal(_left, ref));
                      if (_notEquals) {
                        vars.add(this.vee.getVHDLName(ref));
                      }
                    }
                  } else {
                    vars.add(this.vee.getVHDLName(ref));
                  }
                }
              }
            }
          }
        }
      }
    }
    for (final String string : vars) {
      Signal _signal = new Signal(string, UnresolvedType.NO_NAME);
      sensitivity.add(_signal);
    }
    return sensitivity;
  }
  
  public HDLVariable resolveVar(final HDLReference reference) {
    if ((reference instanceof HDLUnresolvedFragment)) {
      throw new RuntimeException("Can not use unresolved fragments");
    }
    return ((HDLResolvedRef) reference).resolveVar().get();
  }
  
  private SequentialStatement createIfStatement(final HDLUnit hUnit, final ProcessStatement ps, final HDLRegisterConfig config, final LinkedList<SequentialStatement> value, final VHDLContext unit) {
    final HDLRegisterConfig key = config.normalize();
    Expression _vHDL = this.vee.toVHDL(key.getClk());
    Signal clk = ((Signal) _vHDL);
    Expression _vHDL_1 = this.vee.toVHDL(key.getRst());
    Signal rst = ((Signal) _vHDL_1);
    ps.getSensitivityList().add(clk);
    EnumerationLiteral activeRst = null;
    HDLRegisterConfig.HDLRegResetActiveType _resetType = key.getResetType();
    boolean _tripleEquals = (_resetType == HDLRegisterConfig.HDLRegResetActiveType.HIGH);
    if (_tripleEquals) {
      activeRst = StdLogic1164.STD_LOGIC_1;
    } else {
      activeRst = StdLogic1164.STD_LOGIC_0;
    }
    Equals _equals = new Equals(rst, activeRst);
    IfStatement rstIfStmnt = new IfStatement(_equals);
    final LinkedList<SequentialStatement> resets = unit.resetStatements.get(key);
    if ((resets != null)) {
      rstIfStmnt.getStatements().addAll(resets);
    }
    FunctionCall edge = null;
    HDLRegisterConfig.HDLRegClockType _clockType = key.getClockType();
    boolean _tripleEquals_1 = (_clockType == HDLRegisterConfig.HDLRegClockType.RISING);
    if (_tripleEquals_1) {
      FunctionCall _functionCall = new FunctionCall(StdLogic1164.RISING_EDGE);
      edge = _functionCall;
    } else {
      FunctionCall _functionCall_1 = new FunctionCall(StdLogic1164.FALLING_EDGE);
      edge = _functionCall_1;
    }
    List<AssociationElement> _parameters = edge.getParameters();
    AssociationElement _associationElement = new AssociationElement(clk);
    _parameters.add(_associationElement);
    HDLRegisterConfig.HDLRegSyncType _syncType = key.getSyncType();
    boolean _tripleEquals_2 = (_syncType == HDLRegisterConfig.HDLRegSyncType.ASYNC);
    if (_tripleEquals_2) {
      ps.getSensitivityList().add(rst);
      final IfStatement.ElsifPart elsifPart = rstIfStmnt.createElsifPart(edge);
      elsifPart.getStatements().addAll(value);
      return rstIfStmnt;
    }
    final IfStatement clkIf = new IfStatement(edge);
    clkIf.getStatements().add(rstIfStmnt);
    rstIfStmnt.getElseStatements().addAll(value);
    return clkIf;
  }
  
  public VhdlFile toVHDL(final HDLPackage obj) {
    final VhdlFile res = new VhdlFile();
    PackageDeclaration pd = null;
    ArrayList<HDLDeclaration> _declarations = obj.getDeclarations();
    for (final HDLDeclaration decl : _declarations) {
      {
        HDLClass _classType = decl.getClassType();
        boolean _equals = Objects.equal(_classType, HDLClass.HDLVariableDeclaration);
        if (_equals) {
          final HDLVariableDeclaration hvd = ((HDLVariableDeclaration) decl);
          if ((pd == null)) {
            VHDLPackageExtension.staticImports(res.getElements());
            String _pkg = obj.getPkg();
            HDLQualifiedName _hDLQualifiedName = new HDLQualifiedName(_pkg);
            String _packageName = this.getPackageName(_hDLQualifiedName);
            PackageDeclaration _packageDeclaration = new PackageDeclaration(_packageName);
            pd = _packageDeclaration;
            res.getElements().add(pd);
          }
          final VHDLContext vhdl = this.vse.toVHDL(hvd, VHDLContext.DEFAULT_CTX);
          ConstantDeclaration _xifexpression = null;
          boolean _isEmpty = vhdl.constants.isEmpty();
          if (_isEmpty) {
            _xifexpression = null;
          } else {
            _xifexpression = vhdl.constants.getFirst();
          }
          ConstantDeclaration first = _xifexpression;
          if ((first == null)) {
            first = vhdl.constantsPkg.getFirst();
            if ((first == null)) {
              throw new IllegalArgumentException("Expected constant declaration but found none!");
            }
          }
          pd.getDeclarations().add(first);
        }
        HDLClass _classType_1 = decl.getClassType();
        boolean _equals_1 = Objects.equal(_classType_1, HDLClass.HDLEnumDeclaration);
        if (_equals_1) {
          final HDLEnumDeclaration hvd_1 = ((HDLEnumDeclaration) decl);
          String _packageName_1 = this.getPackageName(FullNameExtension.fullNameOf(hvd_1.getHEnum()));
          final PackageDeclaration enumPd = new PackageDeclaration(_packageName_1);
          res.getElements().add(enumPd);
          final VHDLContext vhdl_1 = this.vse.toVHDL(hvd_1, VHDLContext.DEFAULT_CTX);
          DeclarativeItemMarker _first = vhdl_1.internalTypes.getFirst();
          final Type first_1 = ((Type) _first);
          if ((first_1 == null)) {
            throw new IllegalArgumentException("Expected enum type declaration but found none!");
          }
          enumPd.getDeclarations().add(first_1);
        }
      }
    }
    ArrayList<HDLUnit> _units = obj.getUnits();
    for (final HDLUnit unit : _units) {
      {
        final ModificationSet ms = new ModificationSet();
        final HDLVariableDeclaration[] hvds = unit.<HDLVariableDeclaration>getAllObjectsOf(HDLVariableDeclaration.class, true);
        for (final HDLVariableDeclaration hvd : hvds) {
          ArrayList<HDLVariable> _variables = hvd.getVariables();
          for (final HDLVariable hvar : _variables) {
            {
              final HDLVariableRef[] refs = hvar.<HDLVariableRef>getAllObjectsOf(HDLVariableRef.class, true);
              for (final HDLVariableRef ref : refs) {
                {
                  final Optional<HDLVariable> resolvedRef = ref.resolveVar();
                  boolean _isPresent = resolvedRef.isPresent();
                  if (_isPresent) {
                    resolvedRef.get().setMeta(VHDLStatementExtension.EXPORT);
                  }
                }
              }
              final String origName = hvar.getName();
              final String name = VHDLUtils.getVHDLName(origName);
              boolean _notEquals = (!Objects.equal(name, origName));
              if (_notEquals) {
                Refactoring.<HDLUnit>renameVariable(hvar, hvar.asRef().skipLast(1).append(name), unit, ms);
              }
            }
          }
        }
        final HDLUnit newUnit = ms.<HDLUnit>apply(unit);
        res.getElements().addAll(this.toVHDL(newUnit));
      }
    }
    return res;
  }
}
