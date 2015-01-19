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
import de.upb.hni.vmagic.DiscreteRange;
import de.upb.hni.vmagic.Range;
import de.upb.hni.vmagic.concurrent.ComponentInstantiation;
import de.upb.hni.vmagic.concurrent.ConcurrentStatement;
import de.upb.hni.vmagic.concurrent.EntityInstantiation;
import de.upb.hni.vmagic.concurrent.ForGenerateStatement;
import de.upb.hni.vmagic.declaration.Component;
import de.upb.hni.vmagic.declaration.ConstantDeclaration;
import de.upb.hni.vmagic.declaration.SignalDeclaration;
import de.upb.hni.vmagic.expression.Aggregate;
import de.upb.hni.vmagic.expression.Expression;
import de.upb.hni.vmagic.expression.Literal;
import de.upb.hni.vmagic.expression.TypeConversion;
import de.upb.hni.vmagic.libraryunit.Entity;
import de.upb.hni.vmagic.literal.CharacterLiteral;
import de.upb.hni.vmagic.object.Constant;
import de.upb.hni.vmagic.object.Signal;
import de.upb.hni.vmagic.object.SignalAssignmentTarget;
import de.upb.hni.vmagic.object.VhdlObject;
import de.upb.hni.vmagic.object.VhdlObjectProvider;
import de.upb.hni.vmagic.statement.CaseStatement;
import de.upb.hni.vmagic.statement.ForStatement;
import de.upb.hni.vmagic.statement.IfStatement;
import de.upb.hni.vmagic.statement.SequentialStatement;
import de.upb.hni.vmagic.statement.SignalAssignment;
import de.upb.hni.vmagic.type.ConstrainedArray;
import de.upb.hni.vmagic.type.EnumerationType;
import de.upb.hni.vmagic.type.SubtypeIndication;
import de.upb.hni.vmagic.type.UnresolvedType;
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
import org.eclipse.xtext.xbase.lib.InputOutput;
import org.eclipse.xtext.xbase.lib.ObjectExtensions;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure1;
import org.pshdl.generator.vhdl.VHDLContext;
import org.pshdl.generator.vhdl.VHDLExpressionExtension;
import org.pshdl.generator.vhdl.VHDLFunctions;
import org.pshdl.generator.vhdl.VHDLPackageExtension;
import org.pshdl.generator.vhdl.VHDLUtils;
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
import org.pshdl.model.HDLUnresolvedFragment;
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
import org.pshdl.model.utils.HDLQualifiedName;
import org.pshdl.model.utils.HDLQuery;
import org.pshdl.model.utils.Insulin;

@SuppressWarnings("all")
public class VHDLStatementExtension {
  public static VHDLStatementExtension INST = new VHDLStatementExtension();
  
  public static VHDLContext vhdlOf(final HDLStatement stmnt, final int pid) {
    return VHDLStatementExtension.INST.toVHDL(stmnt, pid);
  }
  
  @Extension
  private VHDLExpressionExtension vee = new VHDLExpressionExtension();
  
  private static HDLObject.GenericMeta<HDLQualifiedName> ORIGINAL_FULLNAME = new HDLObject.GenericMeta<HDLQualifiedName>(
    "ORIGINAL_FULLNAME", true);
  
  public static HDLObject.GenericMeta<Boolean> EXPORT = new HDLObject.GenericMeta<Boolean>("EXPORT", true);
  
  protected VHDLContext _toVHDL(final IHDLObject obj, final int pid) {
    HDLClass _classType = obj.getClassType();
    String _plus = ("Not correctly implemented:" + _classType);
    String _plus_1 = (_plus + " ");
    String _plus_2 = (_plus_1 + obj);
    throw new IllegalArgumentException(_plus_2);
  }
  
  protected VHDLContext _toVHDL(final HDLExport obj, final int pid) {
    return new VHDLContext();
  }
  
  protected VHDLContext _toVHDL(final HDLDirectGeneration obj, final int pid) {
    return new VHDLContext();
  }
  
  protected VHDLContext _toVHDL(final HDLFunctionCall obj, final int pid) {
    return VHDLFunctions.toOutputStatement(obj, pid);
  }
  
  protected VHDLContext _toVHDL(final HDLBlock obj, final int pid) {
    final VHDLContext res = new VHDLContext();
    boolean process = false;
    boolean _and = false;
    Boolean _process = obj.getProcess();
    boolean _tripleNotEquals = (_process != null);
    if (!_tripleNotEquals) {
      _and = false;
    } else {
      Boolean _process_1 = obj.getProcess();
      _and = (_process_1).booleanValue();
    }
    if (_and) {
      process = true;
    }
    int _xifexpression = (int) 0;
    if (process) {
      _xifexpression = res.newProcessID();
    } else {
      _xifexpression = pid;
    }
    final int newPid = _xifexpression;
    ArrayList<HDLStatement> _statements = obj.getStatements();
    for (final HDLStatement stmnt : _statements) {
      VHDLContext _vHDL = this.toVHDL(stmnt, newPid);
      res.merge(_vHDL, false);
    }
    return this.attachComment(res, obj);
  }
  
  public VHDLContext attachComment(final VHDLContext context, final IHDLObject block) {
    try {
      final SourceInfo srcInfo = block.<SourceInfo>getMeta(SourceInfo.INFO);
      boolean _tripleNotEquals = (srcInfo != null);
      if (_tripleNotEquals) {
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
                String _substring = newComment.substring(2);
                docComments.add(_substring);
              } else {
                String _substring_1 = newComment.substring(1);
                docComments.add(_substring_1);
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
                String _substring_2 = newComment_1.substring(2);
                String[] _split = _substring_2.split("\n");
                CollectionExtensions.<String>addAll(docComments, _split);
              } else {
                String _substring_3 = newComment_1.substring(1);
                String[] _split_1 = _substring_3.split("\n");
                CollectionExtensions.<String>addAll(docComments, _split_1);
              }
            } else {
              String[] _split_2 = newComment_1.split("\n");
              CollectionExtensions.<String>addAll(newComments, _split_2);
            }
          }
        }
        boolean _or = false;
        boolean _isEmpty = newComments.isEmpty();
        boolean _not = (!_isEmpty);
        if (_not) {
          _or = true;
        } else {
          boolean _isEmpty_1 = docComments.isEmpty();
          boolean _not_1 = (!_isEmpty_1);
          _or = _not_1;
        }
        if (_or) {
          context.attachComments(newComments, docComments);
        }
      }
    } catch (final Throwable _t) {
      if (_t instanceof Exception) {
        final Exception e = (Exception)_t;
      } else {
        throw Exceptions.sneakyThrow(_t);
      }
    }
    return context;
  }
  
  protected VHDLContext _toVHDL(final HDLEnumDeclaration obj, final int pid) {
    final VHDLContext res = new VHDLContext();
    final HDLEnum hEnum = obj.getHEnum();
    final List<String> enums = new LinkedList<String>();
    ArrayList<HDLVariable> _enums = hEnum.getEnums();
    for (final HDLVariable hVar : _enums) {
      String _name = hVar.getName();
      enums.add(_name);
    }
    final String[] enumArr = ((String[])Conversions.unwrapArray(enums, String.class));
    String _name_1 = hEnum.getName();
    EnumerationType _enumerationType = new EnumerationType(_name_1, enumArr);
    res.addTypeDeclaration(_enumerationType, false);
    return this.attachComment(res, obj);
  }
  
  protected VHDLContext _toVHDL(final HDLInterfaceDeclaration obj, final int pid) {
    return new VHDLContext();
  }
  
  private static EnumSet<HDLVariableDeclaration.HDLDirection> inAndOut = EnumSet.<HDLVariableDeclaration.HDLDirection>of(HDLVariableDeclaration.HDLDirection.IN, HDLVariableDeclaration.HDLDirection.INOUT, HDLVariableDeclaration.HDLDirection.OUT);
  
  protected VHDLContext _toVHDL(final HDLInterfaceInstantiation hii, final int pid) {
    final VHDLContext res = new VHDLContext();
    Optional<HDLInterface> _resolveHIf = hii.resolveHIf();
    final HDLInterface hIf = _resolveHIf.get();
    final HDLVariable interfaceVar = hii.getVar();
    HDLVariable _var = hii.getVar();
    final String ifName = _var.getName();
    final HDLQualifiedName asRef = hIf.asRef();
    final HDLInterfaceDeclaration hid = hIf.<HDLInterfaceDeclaration>getContainer(HDLInterfaceDeclaration.class);
    List<AssociationElement> portMap = null;
    List<AssociationElement> genericMap = null;
    ConcurrentStatement instantiation = null;
    final ArrayList<HDLVariableDeclaration> ports = hIf.getPorts();
    boolean _and = false;
    boolean _tripleNotEquals = (hid != null);
    if (!_tripleNotEquals) {
      _and = false;
    } else {
      HDLAnnotation _annotation = hid.getAnnotation(HDLBuiltInAnnotationProvider.HDLBuiltInAnnotations.VHDLComponent);
      boolean _tripleNotEquals_1 = (_annotation != null);
      _and = _tripleNotEquals_1;
    }
    if (_and) {
      final HDLAnnotation anno = hid.getAnnotation(HDLBuiltInAnnotationProvider.HDLBuiltInAnnotations.VHDLComponent);
      String _value = null;
      if (anno!=null) {
        _value=anno.getValue();
      }
      boolean _equals = "declare".equals(_value);
      if (_equals) {
        String _lastSegment = asRef.getLastSegment();
        String _string = _lastSegment.toString();
        final Component c = new Component(_string);
        final VHDLContext cContext = new VHDLContext();
        for (final HDLVariableDeclaration port : ports) {
          VHDLContext _vHDL = this.toVHDL(port, (-1));
          cContext.merge(_vHDL, true);
        }
        for (final Signal signal : cContext.ports) {
          List<VhdlObjectProvider<Signal>> _port = c.getPort();
          _port.add(signal);
        }
        for (final ConstantDeclaration cd : cContext.constants) {
          List<Constant> _objects = cd.getObjects();
          for (final Object vobj : _objects) {
            List<VhdlObjectProvider<Constant>> _generic = c.getGeneric();
            _generic.add(((Constant) vobj));
          }
        }
        for (final Constant constant : cContext.generics) {
          List<VhdlObjectProvider<Constant>> _generic_1 = c.getGeneric();
          _generic_1.add(constant);
        }
        res.addComponent(c);
      } else {
        HDLQualifiedName _nameRef = VHDLPackageExtension.INST.getNameRef(asRef);
        res.addImport(_nameRef);
      }
      String _lastSegment_1 = asRef.getLastSegment();
      String _string_1 = _lastSegment_1.toString();
      final Component entity = new Component(_string_1);
      final ComponentInstantiation inst = new ComponentInstantiation(ifName, entity);
      List<AssociationElement> _portMap = inst.getPortMap();
      portMap = _portMap;
      List<AssociationElement> _genericMap = inst.getGenericMap();
      genericMap = _genericMap;
      instantiation = inst;
    } else {
      HDLQualifiedName _nameRef_1 = VHDLPackageExtension.INST.getNameRef(asRef);
      String _string_2 = _nameRef_1.toString();
      final Entity entity_1 = new Entity(_string_2);
      final EntityInstantiation inst_1 = new EntityInstantiation(ifName, entity_1);
      List<AssociationElement> _portMap_1 = inst_1.getPortMap();
      portMap = _portMap_1;
      List<AssociationElement> _genericMap_1 = inst_1.getGenericMap();
      genericMap = _genericMap_1;
      instantiation = inst_1;
    }
    for (final HDLVariableDeclaration hvd : ports) {
      HDLVariableDeclaration.HDLDirection _direction = hvd.getDirection();
      boolean _contains = VHDLStatementExtension.inAndOut.contains(_direction);
      if (_contains) {
        this.generatePortMap(hvd, ifName, interfaceVar, asRef, res, hii, pid, portMap);
      } else {
        HDLVariableDeclaration.HDLDirection _direction_1 = hvd.getDirection();
        boolean _equals_1 = Objects.equal(_direction_1, HDLVariableDeclaration.HDLDirection.PARAMETER);
        if (_equals_1) {
          ArrayList<HDLVariable> _variables = hvd.getVariables();
          for (final HDLVariable hvar : _variables) {
            {
              HDLVariable sigVar = hvar;
              String _meta = hvar.<String>getMeta(HDLInterfaceInstantiation.ORIG_NAME);
              boolean _tripleNotEquals_2 = (_meta != null);
              if (_tripleNotEquals_2) {
                String _meta_1 = hvar.<String>getMeta(HDLInterfaceInstantiation.ORIG_NAME);
                HDLVariable _setName = hvar.setName(_meta_1);
                sigVar = _setName;
              }
              final HDLVariableRef ref = hvar.asHDLRef();
              String _name = sigVar.getName();
              Expression _vHDL_1 = this.vee.toVHDL(ref);
              AssociationElement _associationElement = new AssociationElement(_name, _vHDL_1);
              genericMap.add(_associationElement);
            }
          }
        }
      }
    }
    ForGenerateStatement forLoop = null;
    ArrayList<HDLExpression> _dimensions = interfaceVar.getDimensions();
    int _size = _dimensions.size();
    boolean _equals_2 = (_size == 0);
    if (_equals_2) {
      res.addConcurrentStatement(instantiation);
    } else {
      int i = 0;
      ArrayList<HDLExpression> _dimensions_1 = interfaceVar.getDimensions();
      for (final HDLExpression exp : _dimensions_1) {
        {
          HDLArithOp _hDLArithOp = new HDLArithOp();
          ArrayList<HDLExpression> _dimensions_2 = interfaceVar.getDimensions();
          HDLExpression _get = _dimensions_2.get(i);
          HDLArithOp _setLeft = _hDLArithOp.setLeft(_get);
          HDLArithOp _setType = _setLeft.setType(
            HDLArithOp.HDLArithOpType.MINUS);
          HDLLiteral _get_1 = HDLLiteral.get(1);
          final HDLExpression to = _setType.setRight(_get_1);
          HDLRange _hDLRange = new HDLRange();
          HDLLiteral _get_2 = HDLLiteral.get(0);
          HDLRange _setFrom = _hDLRange.setFrom(_get_2);
          HDLRange _setTo = _setFrom.setTo(to);
          final HDLRange range = _setTo.setContainer(hii);
          String _asIndex = this.asIndex(Integer.valueOf(i));
          Range _vHDL_1 = this.vee.toVHDL(range, Range.Direction.TO);
          final ForGenerateStatement newFor = new ForGenerateStatement(("generate_" + ifName), _asIndex, _vHDL_1);
          boolean _tripleNotEquals_2 = (forLoop != null);
          if (_tripleNotEquals_2) {
            List<ConcurrentStatement> _statements = forLoop.getStatements();
            _statements.add(newFor);
          } else {
            res.addConcurrentStatement(newFor);
          }
          forLoop = newFor;
          i = (i + 1);
        }
      }
      boolean _tripleEquals = (forLoop == null);
      if (_tripleEquals) {
        throw new IllegalArgumentException("Should not get here");
      }
      List<ConcurrentStatement> _statements = forLoop.getStatements();
      _statements.add(instantiation);
    }
    return this.attachComment(res, hii);
  }
  
  public void generatePortMap(final HDLVariableDeclaration hvd, final String ifName, final HDLVariable interfaceVar, final HDLQualifiedName asRef, final VHDLContext res, final HDLInterfaceInstantiation obj, final int pid, final List<AssociationElement> portMap) {
    HDLQuery.Source<HDLAnnotation> _select = HDLQuery.<HDLAnnotation>select(HDLAnnotation.class);
    HDLQuery.Selector<HDLAnnotation> _from = _select.from(hvd);
    HDLQuery.FieldSelector<HDLAnnotation, String> _where = _from.<String>where(
      HDLAnnotation.fName);
    String _string = HDLBuiltInAnnotationProvider.HDLBuiltInAnnotations.VHDLType.toString();
    HDLQuery.Result<HDLAnnotation, String> _isEqualTo = _where.isEqualTo(_string);
    final Collection<HDLAnnotation> typeAnno = _isEqualTo.getAll();
    ArrayList<HDLVariable> _variables = hvd.getVariables();
    for (final HDLVariable hvar : _variables) {
      {
        HDLVariable sigVar = null;
        HDLAnnotation _annotation = hvar.getAnnotation(HDLBuiltInAnnotationProvider.HDLBuiltInAnnotations.exportedSignal);
        boolean _tripleNotEquals = (_annotation != null);
        if (_tripleNotEquals) {
          HDLVariable _hDLVariable = new HDLVariable();
          String _name = hvar.getName();
          HDLVariable _setName = _hDLVariable.setName(_name);
          sigVar = _setName;
          HDLVariableRef ref = sigVar.asHDLRef();
          String _name_1 = hvar.getName();
          String _vHDLName = VHDLUtils.getVHDLName(_name_1);
          Expression _vHDL = this.vee.toVHDL(ref);
          AssociationElement _associationElement = new AssociationElement(_vHDLName, _vHDL);
          portMap.add(_associationElement);
        } else {
          String _name_2 = hvar.getName();
          String _mapName = VHDLUtils.mapName(ifName, _name_2);
          HDLVariable _setName_1 = hvar.setName(_mapName);
          sigVar = _setName_1;
          HDLVariableRef ref_1 = sigVar.asHDLRef();
          int i = 0;
          ArrayList<HDLExpression> _dimensions = interfaceVar.getDimensions();
          for (final HDLExpression exp : _dimensions) {
            {
              HDLVariableRef _hDLVariableRef = new HDLVariableRef();
              String _asIndex = this.asIndex(Integer.valueOf(i));
              HDLQualifiedName _create = HDLQualifiedName.create(_asIndex);
              HDLVariableRef _setVar = _hDLVariableRef.setVar(_create);
              HDLVariableRef _addArray = ref_1.addArray(_setVar);
              ref_1 = _addArray;
              i = (i + 1);
            }
          }
          ArrayList<HDLExpression> _dimensions_1 = interfaceVar.getDimensions();
          for (final HDLExpression exp_1 : _dimensions_1) {
            HDLVariable _addDimensions = sigVar.addDimensions(exp_1);
            sigVar = _addDimensions;
          }
          ArrayList<HDLExpression> _dimensions_2 = hvar.getDimensions();
          int _size = _dimensions_2.size();
          boolean _notEquals = (_size != 0);
          if (_notEquals) {
            boolean _isEmpty = typeAnno.isEmpty();
            if (_isEmpty) {
              HDLQualifiedName _packageNameRef = VHDLPackageExtension.INST.getPackageNameRef(asRef);
              String _arrayRefName = VHDLStatementExtension.getArrayRefName(hvar, true);
              final HDLQualifiedName name = _packageNameRef.append(_arrayRefName);
              res.addImport(name);
              HDLVariableDeclaration _setDirection = hvd.setDirection(HDLVariableDeclaration.HDLDirection.INTERNAL);
              HDLVariable _setDimensions = sigVar.setDimensions(null);
              String _string_1 = name.toString();
              HDLAnnotation _create = HDLBuiltInAnnotationProvider.HDLBuiltInAnnotations.VHDLType.create(_string_1);
              HDLVariable _addAnnotations = _setDimensions.addAnnotations(_create);
              Iterable<HDLVariable> _asList = HDLObject.<HDLVariable>asList(_addAnnotations);
              HDLVariableDeclaration _setVariables = _setDirection.setVariables(_asList);
              final HDLVariableDeclaration newHVD = _setVariables.copyDeepFrozen(obj);
              VHDLContext _vHDL_1 = this.toVHDL(newHVD, pid);
              res.merge(_vHDL_1, false);
            } else {
              HDLVariableDeclaration _setDirection_1 = hvd.setDirection(HDLVariableDeclaration.HDLDirection.INTERNAL);
              HDLVariable _setDimensions_1 = sigVar.setDimensions(null);
              Iterable<HDLVariable> _asList_1 = HDLObject.<HDLVariable>asList(_setDimensions_1);
              HDLVariableDeclaration _setVariables_1 = _setDirection_1.setVariables(_asList_1);
              final HDLVariableDeclaration newHVD_1 = _setVariables_1.copyDeepFrozen(obj);
              VHDLContext _vHDL_2 = this.toVHDL(newHVD_1, pid);
              res.merge(_vHDL_2, false);
            }
          } else {
            HDLVariableDeclaration _setDirection_2 = hvd.setDirection(HDLVariableDeclaration.HDLDirection.INTERNAL);
            Iterable<HDLVariable> _asList_2 = HDLObject.<HDLVariable>asList(sigVar);
            HDLVariableDeclaration _setVariables_2 = _setDirection_2.setVariables(_asList_2);
            final HDLVariableDeclaration newHVD_2 = _setVariables_2.copyDeepFrozen(obj);
            VHDLContext _vHDL_3 = this.toVHDL(newHVD_2, pid);
            res.merge(_vHDL_3, false);
          }
          String _name_3 = hvar.getName();
          String _vHDLName_1 = VHDLUtils.getVHDLName(_name_3);
          Expression _vHDL_4 = this.vee.toVHDL(ref_1);
          AssociationElement _associationElement_1 = new AssociationElement(_vHDLName_1, _vHDL_4);
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
      HDLQualifiedName _meta = hvar.<HDLQualifiedName>getMeta(VHDLStatementExtension.ORIGINAL_FULLNAME);
      boolean _tripleNotEquals = (_meta != null);
      if (_tripleNotEquals) {
        HDLQualifiedName _meta_1 = hvar.<HDLQualifiedName>getMeta(VHDLStatementExtension.ORIGINAL_FULLNAME);
        fullName = _meta_1;
      } else {
        HDLQualifiedName _fullNameOf = FullNameExtension.fullNameOf(hvar);
        fullName = _fullNameOf;
      }
      String _string = fullName.toString('_');
      res = _string;
    } else {
      String _name = hvar.getName();
      res = _name;
    }
    String _unescapeVHDLName = VHDLUtils.unescapeVHDLName(res);
    String _plus = (_unescapeVHDLName + "_array");
    return VHDLUtils.getVHDLName(_plus);
  }
  
  protected VHDLContext _toVHDL(final HDLVariableDeclaration obj, final int pid) {
    final VHDLContext res = new VHDLContext();
    final HDLPrimitive primitive = obj.getPrimitive();
    SubtypeIndication type = null;
    HDLExpression resetValue = null;
    HDLQuery.Source<HDLAnnotation> _select = HDLQuery.<HDLAnnotation>select(HDLAnnotation.class);
    HDLQuery.Selector<HDLAnnotation> _from = _select.from(obj);
    HDLQuery.FieldSelector<HDLAnnotation, String> _where = _from.<String>where(HDLAnnotation.fName);
    String _string = HDLBuiltInAnnotationProvider.HDLBuiltInAnnotations.VHDLType.toString();
    HDLQuery.Result<HDLAnnotation, String> _isEqualTo = _where.isEqualTo(_string);
    final HDLAnnotation typeAnno = _isEqualTo.getFirst();
    HDLRegisterConfig _register = obj.getRegister();
    boolean _tripleNotEquals = (_register != null);
    if (_tripleNotEquals) {
      HDLRegisterConfig _register_1 = obj.getRegister();
      HDLExpression _resetValue = _register_1.getResetValue();
      resetValue = _resetValue;
    }
    char _charAt = "0".charAt(0);
    CharacterLiteral _characterLiteral = new CharacterLiteral(_charAt);
    Expression otherValue = Aggregate.OTHERS(_characterLiteral);
    boolean _tripleNotEquals_1 = (typeAnno != null);
    if (_tripleNotEquals_1) {
      String _value = typeAnno.getValue();
      final HDLQualifiedName value = new HDLQualifiedName(_value);
      res.addImport(value);
      String _lastSegment = value.getLastSegment();
      EnumerationType _enumerationType = new EnumerationType(_lastSegment);
      type = _enumerationType;
    } else {
      boolean _tripleNotEquals_2 = (primitive != null);
      if (_tripleNotEquals_2) {
        SubtypeIndication _type = VHDLCastsLibrary.getType(primitive);
        type = _type;
      } else {
        Optional<? extends HDLType> _resolveType = obj.resolveType();
        final HDLType hType = _resolveType.get();
        if ((hType instanceof HDLEnum)) {
          final HDLEnum hEnum = ((HDLEnum) hType);
          String _name = hEnum.getName();
          EnumerationType _enumerationType_1 = new EnumerationType(_name);
          type = _enumerationType_1;
          int idx = 0;
          HDLEvaluationContext _hDLEvaluationContext = new HDLEvaluationContext();
          final Procedure1<HDLEvaluationContext> _function = new Procedure1<HDLEvaluationContext>() {
            public void apply(final HDLEvaluationContext it) {
              it.enumAsInt = true;
            }
          };
          HDLEvaluationContext _doubleArrow = ObjectExtensions.<HDLEvaluationContext>operator_doubleArrow(_hDLEvaluationContext, _function);
          final Optional<BigInteger> resVal = ConstantEvaluate.valueOf(resetValue, _doubleArrow);
          boolean _isPresent = resVal.isPresent();
          if (_isPresent) {
            BigInteger _get = resVal.get();
            int _intValue = _get.intValue();
            idx = _intValue;
          }
          HDLEnumRef _hDLEnumRef = new HDLEnumRef();
          HDLQualifiedName _asRef = hEnum.asRef();
          HDLEnumRef _setHEnum = _hDLEnumRef.setHEnum(_asRef);
          ArrayList<HDLVariable> _enums = hEnum.getEnums();
          HDLVariable _get_1 = _enums.get(idx);
          HDLQualifiedName _asRef_1 = _get_1.asRef();
          final HDLEnumRef enumReset = _setHEnum.setVar(_asRef_1);
          Expression _vHDL = this.vee.toVHDL(enumReset);
          otherValue = _vHDL;
          if ((!(resetValue instanceof HDLArrayInit))) {
            resetValue = enumReset;
          }
        }
      }
    }
    boolean _tripleNotEquals_3 = (type != null);
    if (_tripleNotEquals_3) {
      ArrayList<HDLVariable> _variables = obj.getVariables();
      for (final HDLVariable hvar : _variables) {
        this.handleVariable(hvar, type, obj, res, resetValue, otherValue, pid);
      }
    }
    return this.attachComment(res, obj);
  }
  
  public void handleVariable(final HDLVariable hvar, final SubtypeIndication type, final HDLVariableDeclaration obj, final VHDLContext res, final HDLExpression resetValue, final Expression otherValue, final int pid) {
    HDLAnnotation _annotation = hvar.getAnnotation(HDLBuiltInAnnotationProvider.HDLBuiltInAnnotations.VHDLNoExplicitReset);
    final boolean noExplicitResetVar = (_annotation != null);
    SubtypeIndication varType = type;
    ArrayList<HDLExpression> _dimensions = hvar.getDimensions();
    int _size = _dimensions.size();
    boolean _notEquals = (_size != 0);
    if (_notEquals) {
      final LinkedList<DiscreteRange> ranges = new LinkedList<DiscreteRange>();
      ArrayList<HDLExpression> _dimensions_1 = hvar.getDimensions();
      for (final HDLExpression arrayWidth : _dimensions_1) {
        {
          HDLArithOp _hDLArithOp = new HDLArithOp();
          HDLArithOp _setLeft = _hDLArithOp.setLeft(arrayWidth);
          HDLArithOp _setType = _setLeft.setType(
            HDLArithOp.HDLArithOpType.MINUS);
          HDLLiteral _get = HDLLiteral.get(1);
          final HDLExpression newWidth = _setType.setRight(_get);
          HDLRange _hDLRange = new HDLRange();
          HDLLiteral _get_1 = HDLLiteral.get(0);
          HDLRange _setFrom = _hDLRange.setFrom(_get_1);
          HDLRange _setTo = _setFrom.setTo(newWidth);
          HDLRange _copyDeepFrozen = _setTo.copyDeepFrozen(obj);
          final Range range = this.vee.toVHDL(_copyDeepFrozen, Range.Direction.TO);
          ranges.add(range);
        }
      }
      final boolean external = obj.isExternal();
      String _arrayRefName = VHDLStatementExtension.getArrayRefName(hvar, external);
      final ConstrainedArray arrType = new ConstrainedArray(_arrayRefName, type, ranges);
      res.addTypeDeclaration(arrType, external);
      varType = arrType;
    }
    String _name = hvar.getName();
    final Signal s = new Signal(_name, varType);
    boolean _and = false;
    boolean _and_1 = false;
    boolean _tripleNotEquals = (resetValue != null);
    if (!_tripleNotEquals) {
      _and_1 = false;
    } else {
      _and_1 = (!noExplicitResetVar);
    }
    if (!_and_1) {
      _and = false;
    } else {
      HDLRegisterConfig _register = obj.getRegister();
      boolean _tripleNotEquals_1 = (_register != null);
      _and = _tripleNotEquals_1;
    }
    if (_and) {
      boolean synchedArray = false;
      if ((resetValue instanceof HDLVariableRef)) {
        final HDLVariableRef ref = ((HDLVariableRef) resetValue);
        Optional<HDLVariable> _resolveVar = ref.resolveVar();
        HDLVariable _get = _resolveVar.get();
        ArrayList<HDLExpression> _dimensions_2 = _get.getDimensions();
        int _size_1 = _dimensions_2.size();
        boolean _notEquals_1 = (_size_1 != 0);
        synchedArray = _notEquals_1;
      }
      HDLVariableRef _hDLVariableRef = new HDLVariableRef();
      HDLQualifiedName _asRef = hvar.asRef();
      final HDLVariableRef target = _hDLVariableRef.setVar(_asRef);
      if ((resetValue instanceof HDLArrayInit)) {
        Expression _vHDLArray = this.vee.toVHDLArray(resetValue, otherValue);
        final SignalAssignment sa = new SignalAssignment(s, _vHDLArray);
        HDLRegisterConfig _register_1 = obj.getRegister();
        res.addResetValue(_register_1, sa);
      } else {
        List<HDLExpression> _emptyList = Collections.<HDLExpression>emptyList();
        ArrayList<HDLExpression> _dimensions_3 = hvar.getDimensions();
        HDLStatement _createArrayForLoop = Insulin.createArrayForLoop(_emptyList, _dimensions_3, 0, resetValue, target, synchedArray);
        final HDLStatement initLoop = _createArrayForLoop.copyDeepFrozen(obj);
        final VHDLContext vhdl = this.toVHDL(initLoop, pid);
        HDLRegisterConfig _register_2 = obj.getRegister();
        SequentialStatement _statement = vhdl.getStatement();
        res.addResetValue(_register_2, _statement);
      }
    }
    String _name_1 = hvar.getName();
    final Constant constant = new Constant(_name_1, varType);
    HDLExpression _defaultValue = hvar.getDefaultValue();
    boolean _tripleNotEquals_2 = (_defaultValue != null);
    if (_tripleNotEquals_2) {
      HDLExpression _defaultValue_1 = hvar.getDefaultValue();
      Expression _vHDLArray_1 = this.vee.toVHDLArray(_defaultValue_1, otherValue);
      constant.setDefaultValue(_vHDLArray_1);
    }
    if (noExplicitResetVar) {
      if ((resetValue instanceof HDLArrayInit)) {
        Expression _vHDLArray_2 = this.vee.toVHDLArray(resetValue, otherValue);
        s.setDefaultValue(_vHDLArray_2);
      } else {
        boolean _tripleNotEquals_3 = (resetValue != null);
        if (_tripleNotEquals_3) {
          Expression assign = this.vee.toVHDL(resetValue);
          ArrayList<HDLExpression> _dimensions_4 = hvar.getDimensions();
          for (final HDLExpression exp : _dimensions_4) {
            Aggregate _OTHERS = Aggregate.OTHERS(assign);
            assign = _OTHERS;
          }
          s.setDefaultValue(assign);
        }
      }
    }
    HDLVariableDeclaration.HDLDirection _direction = obj.getDirection();
    boolean _matched = false;
    if (!_matched) {
      if (Objects.equal(_direction, HDLVariableDeclaration.HDLDirection.IN)) {
        _matched=true;
        s.setMode(VhdlObject.Mode.IN);
        res.addPortDeclaration(s);
      }
    }
    if (!_matched) {
      if (Objects.equal(_direction, HDLVariableDeclaration.HDLDirection.OUT)) {
        _matched=true;
        s.setMode(VhdlObject.Mode.OUT);
        res.addPortDeclaration(s);
      }
    }
    if (!_matched) {
      if (Objects.equal(_direction, HDLVariableDeclaration.HDLDirection.INOUT)) {
        _matched=true;
        s.setMode(VhdlObject.Mode.INOUT);
        res.addPortDeclaration(s);
      }
    }
    if (!_matched) {
      if (Objects.equal(_direction, HDLVariableDeclaration.HDLDirection.INTERNAL)) {
        _matched=true;
        final SignalDeclaration sd = new SignalDeclaration(s);
        res.addInternalSignalDeclaration(sd);
      }
    }
    if (!_matched) {
      boolean _or = false;
      HDLVariableDeclaration.HDLDirection _direction_1 = obj.getDirection();
      boolean _equals = Objects.equal(_direction_1, HDLVariableDeclaration.HDLDirection.HIDDEN);
      if (_equals) {
        _or = true;
      } else {
        HDLVariableDeclaration.HDLDirection _direction_2 = obj.getDirection();
        boolean _equals_1 = Objects.equal(_direction_2, HDLVariableDeclaration.HDLDirection.CONSTANT);
        _or = _equals_1;
      }
      if (_or) {
        _matched=true;
        final ConstantDeclaration cd = new ConstantDeclaration(constant);
        boolean _hasMeta = hvar.hasMeta(VHDLStatementExtension.EXPORT);
        if (_hasMeta) {
          res.addConstantDeclarationPkg(cd);
        } else {
          res.addConstantDeclaration(cd);
        }
      }
    }
    if (!_matched) {
      if (Objects.equal(_direction, HDLVariableDeclaration.HDLDirection.PARAMETER)) {
        _matched=true;
        res.addGenericDeclaration(constant);
      }
    }
  }
  
  protected VHDLContext _toVHDL(final HDLSwitchStatement obj, final int pid) {
    final VHDLContext context = new VHDLContext();
    final HDLExpression hCaseExp = obj.getCaseExp();
    Optional<BigInteger> width = Optional.<BigInteger>absent();
    final Optional<? extends HDLType> type = TypeExtension.typeOf(hCaseExp);
    boolean _and = false;
    boolean _isPresent = type.isPresent();
    if (!_isPresent) {
      _and = false;
    } else {
      HDLType _get = type.get();
      _and = (_get instanceof HDLPrimitive);
    }
    if (_and) {
      HDLType _get_1 = type.get();
      HDLExpression _width = ((HDLPrimitive) _get_1).getWidth();
      Optional<BigInteger> _valueOf = ConstantEvaluate.valueOf(_width, null);
      width = _valueOf;
      boolean _isPresent_1 = width.isPresent();
      boolean _not = (!_isPresent_1);
      if (_not) {
        throw new IllegalArgumentException("HDLPrimitive switch case needs to have constant width");
      }
    }
    final Expression caseExp = this.vee.toVHDL(hCaseExp);
    final Map<HDLSwitchCaseStatement, VHDLContext> ctxs = new LinkedHashMap<HDLSwitchCaseStatement, VHDLContext>();
    final Set<HDLRegisterConfig> configs = new LinkedHashSet<HDLRegisterConfig>();
    boolean hasUnclocked = false;
    ArrayList<HDLSwitchCaseStatement> _cases = obj.getCases();
    for (final HDLSwitchCaseStatement cs : _cases) {
      {
        final VHDLContext vhdl = this.toVHDL(cs, pid);
        ctxs.put(cs, vhdl);
        int _size = vhdl.unclockedStatements.size();
        boolean _greaterThan = (_size > 0);
        if (_greaterThan) {
          hasUnclocked = true;
        }
        Set<HDLRegisterConfig> _keySet = vhdl.clockedStatements.keySet();
        configs.addAll(_keySet);
      }
    }
    for (final HDLRegisterConfig hdlRegisterConfig : configs) {
      {
        final CaseStatement cs_1 = new CaseStatement(caseExp);
        Set<Map.Entry<HDLSwitchCaseStatement, VHDLContext>> _entrySet = ctxs.entrySet();
        for (final Map.Entry<HDLSwitchCaseStatement, VHDLContext> e : _entrySet) {
          {
            final CaseStatement.Alternative alt = this.createAlternative(cs_1, e, width);
            VHDLContext _value = e.getValue();
            final LinkedList<SequentialStatement> clockCase = _value.clockedStatements.get(hdlRegisterConfig);
            boolean _tripleNotEquals = (clockCase != null);
            if (_tripleNotEquals) {
              List<SequentialStatement> _statements = alt.getStatements();
              _statements.addAll(clockCase);
            }
          }
        }
        context.addClockedStatement(hdlRegisterConfig, cs_1);
      }
    }
    if (hasUnclocked) {
      final CaseStatement cs_1 = new CaseStatement(caseExp);
      Set<Map.Entry<HDLSwitchCaseStatement, VHDLContext>> _entrySet = ctxs.entrySet();
      for (final Map.Entry<HDLSwitchCaseStatement, VHDLContext> e : _entrySet) {
        {
          final CaseStatement.Alternative alt = this.createAlternative(cs_1, e, width);
          VHDLContext _value = e.getValue();
          LinkedList<SequentialStatement> _get_2 = _value.unclockedStatements.get(Integer.valueOf(pid));
          boolean _tripleNotEquals = (_get_2 != null);
          if (_tripleNotEquals) {
            List<SequentialStatement> _statements = alt.getStatements();
            VHDLContext _value_1 = e.getValue();
            LinkedList<SequentialStatement> _get_3 = _value_1.unclockedStatements.get(Integer.valueOf(pid));
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
    HDLSwitchCaseStatement _key = e.getKey();
    final HDLExpression label = _key.getLabel();
    boolean _tripleNotEquals = (label != null);
    if (_tripleNotEquals) {
      final Optional<BigInteger> eval = ConstantEvaluate.valueOf(label, null);
      boolean _isPresent = eval.isPresent();
      if (_isPresent) {
        boolean _isPresent_1 = bits.isPresent();
        boolean _not = (!_isPresent_1);
        if (_not) {
          throw new IllegalArgumentException("The width needs to be known for primitive types!");
        }
        BigInteger _get = bits.get();
        int _intValue = _get.intValue();
        BigInteger _get_1 = eval.get();
        Literal _binaryLiteral = VHDLUtils.toBinaryLiteral(_intValue, _get_1);
        CaseStatement.Alternative _createAlternative = cs.createAlternative(_binaryLiteral);
        alt = _createAlternative;
      } else {
        Expression _vHDL = this.vee.toVHDL(label);
        CaseStatement.Alternative _createAlternative_1 = cs.createAlternative(_vHDL);
        alt = _createAlternative_1;
      }
    } else {
      CaseStatement.Alternative _createAlternative_2 = cs.createAlternative(Choices.OTHERS);
      alt = _createAlternative_2;
    }
    return alt;
  }
  
  protected VHDLContext _toVHDL(final HDLSwitchCaseStatement obj, final int pid) {
    final VHDLContext res = new VHDLContext();
    ArrayList<HDLStatement> _dos = obj.getDos();
    for (final HDLStatement stmnt : _dos) {
      VHDLContext _vHDL = this.toVHDL(stmnt, pid);
      res.merge(_vHDL, false);
    }
    return this.attachComment(res, obj);
  }
  
  protected VHDLContext _toVHDL(final HDLAssignment obj, final int pid) {
    final VHDLContext context = new VHDLContext();
    SignalAssignment sa = null;
    final HDLReference ref = obj.getLeft();
    String _string = ref.toString();
    boolean _equals = Objects.equal(_string, "wrapper.T1");
    if (_equals) {
      String _string_1 = ref.toString();
      InputOutput.<String>println(_string_1);
    }
    final HDLVariable hvar = this.resolveVar(ref);
    final ArrayList<HDLExpression> dim = hvar.getDimensions();
    boolean _and = false;
    int _size = dim.size();
    boolean _notEquals = (_size != 0);
    if (!_notEquals) {
      _and = false;
    } else {
      HDLClass _classType = ref.getClassType();
      boolean _equals_1 = Objects.equal(_classType, HDLClass.HDLVariableRef);
      _and = _equals_1;
    }
    if (_and) {
      final HDLVariableRef varRef = ((HDLVariableRef) ref);
      ArrayList<HDLExpression> _array = varRef.getArray();
      for (final HDLExpression exp : _array) {
        dim.remove(0);
      }
      boolean _and_1 = false;
      int _size_1 = dim.size();
      boolean _notEquals_1 = (_size_1 != 0);
      if (!_notEquals_1) {
        _and_1 = false;
      } else {
        HDLExpression _right = obj.getRight();
        HDLClass _classType_1 = _right.getClassType();
        boolean _notEquals_2 = (!Objects.equal(_classType_1, HDLClass.HDLArrayInit));
        _and_1 = _notEquals_2;
      }
      if (_and_1) {
        final HDLAnnotation typeAnno = hvar.getAnnotation(HDLBuiltInAnnotationProvider.HDLBuiltInAnnotations.VHDLType);
        boolean _tripleNotEquals = (typeAnno != null);
        if (_tripleNotEquals) {
          Expression _vHDL = this.vee.toVHDL(ref);
          String _value = typeAnno.getValue();
          UnresolvedType _unresolvedType = new UnresolvedType(_value);
          HDLExpression _right_1 = obj.getRight();
          Expression _vHDL_1 = this.vee.toVHDL(_right_1);
          TypeConversion _typeConversion = new TypeConversion(_unresolvedType, _vHDL_1);
          SignalAssignment _signalAssignment = new SignalAssignment(((SignalAssignmentTarget) _vHDL), _typeConversion);
          sa = _signalAssignment;
        } else {
          final HDLVariableDeclaration hvd = hvar.<HDLVariableDeclaration>getContainer(HDLVariableDeclaration.class);
          Expression _vHDL_2 = this.vee.toVHDL(ref);
          boolean _isExternal = hvd.isExternal();
          String _arrayRefName = VHDLStatementExtension.getArrayRefName(hvar, _isExternal);
          UnresolvedType _unresolvedType_1 = new UnresolvedType(_arrayRefName);
          HDLExpression _right_2 = obj.getRight();
          Expression _vHDL_3 = this.vee.toVHDL(_right_2);
          TypeConversion _typeConversion_1 = new TypeConversion(_unresolvedType_1, _vHDL_3);
          SignalAssignment _signalAssignment_1 = new SignalAssignment(((SignalAssignmentTarget) _vHDL_2), _typeConversion_1);
          sa = _signalAssignment_1;
        }
      } else {
        Expression _vHDL_4 = this.vee.toVHDL(ref);
        HDLExpression _right_3 = obj.getRight();
        Expression _vHDL_5 = this.vee.toVHDL(_right_3);
        SignalAssignment _signalAssignment_2 = new SignalAssignment(((SignalAssignmentTarget) _vHDL_4), _vHDL_5);
        sa = _signalAssignment_2;
      }
    } else {
      Expression _vHDL_6 = this.vee.toVHDL(ref);
      HDLExpression _right_4 = obj.getRight();
      Expression _vHDL_7 = this.vee.toVHDL(_right_4);
      SignalAssignment _signalAssignment_3 = new SignalAssignment(((SignalAssignmentTarget) _vHDL_6), _vHDL_7);
      sa = _signalAssignment_3;
    }
    final HDLRegisterConfig config = hvar.getRegisterConfig();
    boolean _tripleNotEquals_1 = (config != null);
    if (_tripleNotEquals_1) {
      context.addClockedStatement(config, sa);
    } else {
      context.addUnclockedStatement(pid, sa, obj);
    }
    return this.attachComment(context, obj);
  }
  
  public HDLVariable resolveVar(final HDLReference reference) {
    if ((reference instanceof HDLUnresolvedFragment)) {
      throw new RuntimeException("Can not use unresolved fragments");
    }
    Optional<HDLVariable> _resolveVar = ((HDLResolvedRef) reference).resolveVar();
    return _resolveVar.get();
  }
  
  protected VHDLContext _toVHDL(final HDLForLoop obj, final int pid) {
    final VHDLContext context = new VHDLContext();
    ArrayList<HDLStatement> _dos = obj.getDos();
    for (final HDLStatement stmnt : _dos) {
      VHDLContext _vHDL = this.toVHDL(stmnt, pid);
      context.merge(_vHDL, false);
    }
    final VHDLContext res = new VHDLContext();
    res.merge(context, true);
    Set<Map.Entry<HDLRegisterConfig, LinkedList<SequentialStatement>>> _entrySet = context.clockedStatements.entrySet();
    for (final Map.Entry<HDLRegisterConfig, LinkedList<SequentialStatement>> e : _entrySet) {
      {
        HDLVariable _param = obj.getParam();
        String _name = _param.getName();
        String _vHDLName = VHDLUtils.getVHDLName(_name);
        ArrayList<HDLRange> _range = obj.getRange();
        HDLRange _get = _range.get(0);
        Range _vHDL_1 = this.vee.toVHDL(_get, Range.Direction.TO);
        final ForStatement fStmnt = new ForStatement(_vHDLName, _vHDL_1);
        List<SequentialStatement> _statements = fStmnt.getStatements();
        LinkedList<SequentialStatement> _value = e.getValue();
        _statements.addAll(_value);
        HDLRegisterConfig _key = e.getKey();
        res.addClockedStatement(_key, fStmnt);
      }
    }
    LinkedList<SequentialStatement> _get = context.unclockedStatements.get(Integer.valueOf(pid));
    boolean _tripleNotEquals = (_get != null);
    if (_tripleNotEquals) {
      HDLVariable _param = obj.getParam();
      String _name = _param.getName();
      String _vHDLName = VHDLUtils.getVHDLName(_name);
      ArrayList<HDLRange> _range = obj.getRange();
      HDLRange _get_1 = _range.get(0);
      Range _vHDL_1 = this.vee.toVHDL(_get_1, Range.Direction.TO);
      final ForStatement fStmnt = new ForStatement(_vHDLName, _vHDL_1);
      List<SequentialStatement> _statements = fStmnt.getStatements();
      LinkedList<SequentialStatement> _get_2 = context.unclockedStatements.get(Integer.valueOf(pid));
      _statements.addAll(_get_2);
      res.addUnclockedStatement(pid, fStmnt, obj);
    }
    return this.attachComment(res, obj);
  }
  
  protected VHDLContext _toVHDL(final HDLIfStatement obj, final int pid) {
    final VHDLContext thenCtx = new VHDLContext();
    ArrayList<HDLStatement> _thenDo = obj.getThenDo();
    for (final HDLStatement stmnt : _thenDo) {
      VHDLContext _vHDL = this.toVHDL(stmnt, pid);
      thenCtx.merge(_vHDL, false);
    }
    final VHDLContext elseCtx = new VHDLContext();
    ArrayList<HDLStatement> _elseDo = obj.getElseDo();
    for (final HDLStatement stmnt_1 : _elseDo) {
      VHDLContext _vHDL_1 = this.toVHDL(stmnt_1, pid);
      elseCtx.merge(_vHDL_1, false);
    }
    final Set<HDLRegisterConfig> configs = new LinkedHashSet<HDLRegisterConfig>();
    Set<HDLRegisterConfig> _keySet = thenCtx.clockedStatements.keySet();
    configs.addAll(_keySet);
    Set<HDLRegisterConfig> _keySet_1 = elseCtx.clockedStatements.keySet();
    configs.addAll(_keySet_1);
    final VHDLContext res = new VHDLContext();
    res.merge(thenCtx, true);
    res.merge(elseCtx, true);
    HDLExpression _ifExp = obj.getIfExp();
    final Expression ifExp = this.vee.toVHDL(_ifExp);
    for (final HDLRegisterConfig config : configs) {
      {
        final IfStatement ifs = new IfStatement(ifExp);
        LinkedList<SequentialStatement> _get = thenCtx.clockedStatements.get(config);
        boolean _tripleNotEquals = (_get != null);
        if (_tripleNotEquals) {
          List<SequentialStatement> _statements = ifs.getStatements();
          LinkedList<SequentialStatement> _get_1 = thenCtx.clockedStatements.get(config);
          _statements.addAll(_get_1);
        }
        LinkedList<SequentialStatement> _get_2 = elseCtx.clockedStatements.get(config);
        boolean _tripleNotEquals_1 = (_get_2 != null);
        if (_tripleNotEquals_1) {
          List<SequentialStatement> _elseStatements = ifs.getElseStatements();
          LinkedList<SequentialStatement> _get_3 = elseCtx.clockedStatements.get(config);
          _elseStatements.addAll(_get_3);
        }
        res.addClockedStatement(config, ifs);
      }
    }
    boolean _or = false;
    int _size = thenCtx.unclockedStatements.size();
    boolean _notEquals = (_size != 0);
    if (_notEquals) {
      _or = true;
    } else {
      int _size_1 = elseCtx.unclockedStatements.size();
      boolean _notEquals_1 = (_size_1 != 0);
      _or = _notEquals_1;
    }
    if (_or) {
      final IfStatement ifs = new IfStatement(ifExp);
      LinkedList<SequentialStatement> _get = thenCtx.unclockedStatements.get(Integer.valueOf(pid));
      boolean _tripleNotEquals = (_get != null);
      if (_tripleNotEquals) {
        List<SequentialStatement> _statements = ifs.getStatements();
        LinkedList<SequentialStatement> _get_1 = thenCtx.unclockedStatements.get(Integer.valueOf(pid));
        _statements.addAll(_get_1);
      }
      LinkedList<SequentialStatement> _get_2 = elseCtx.unclockedStatements.get(Integer.valueOf(pid));
      boolean _tripleNotEquals_1 = (_get_2 != null);
      if (_tripleNotEquals_1) {
        List<SequentialStatement> _elseStatements = ifs.getElseStatements();
        LinkedList<SequentialStatement> _get_3 = elseCtx.unclockedStatements.get(Integer.valueOf(pid));
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
    if (obj instanceof HDLBlock) {
      return _toVHDL((HDLBlock)obj, pid);
    } else if (obj instanceof HDLDirectGeneration) {
      return _toVHDL((HDLDirectGeneration)obj, pid);
    } else if (obj instanceof HDLEnumDeclaration) {
      return _toVHDL((HDLEnumDeclaration)obj, pid);
    } else if (obj instanceof HDLForLoop) {
      return _toVHDL((HDLForLoop)obj, pid);
    } else if (obj instanceof HDLFunction) {
      return _toVHDL((HDLFunction)obj, pid);
    } else if (obj instanceof HDLIfStatement) {
      return _toVHDL((HDLIfStatement)obj, pid);
    } else if (obj instanceof HDLInterfaceDeclaration) {
      return _toVHDL((HDLInterfaceDeclaration)obj, pid);
    } else if (obj instanceof HDLInterfaceInstantiation) {
      return _toVHDL((HDLInterfaceInstantiation)obj, pid);
    } else if (obj instanceof HDLSwitchCaseStatement) {
      return _toVHDL((HDLSwitchCaseStatement)obj, pid);
    } else if (obj instanceof HDLSwitchStatement) {
      return _toVHDL((HDLSwitchStatement)obj, pid);
    } else if (obj instanceof HDLVariableDeclaration) {
      return _toVHDL((HDLVariableDeclaration)obj, pid);
    } else if (obj instanceof HDLAssignment) {
      return _toVHDL((HDLAssignment)obj, pid);
    } else if (obj instanceof HDLExport) {
      return _toVHDL((HDLExport)obj, pid);
    } else if (obj instanceof HDLFunctionCall) {
      return _toVHDL((HDLFunctionCall)obj, pid);
    } else if (obj != null) {
      return _toVHDL(obj, pid);
    } else {
      throw new IllegalArgumentException("Unhandled parameter types: " +
        Arrays.<Object>asList(obj, pid).toString());
    }
  }
}
