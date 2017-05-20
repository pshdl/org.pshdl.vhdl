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
import de.upb.hni.vmagic.declaration.VariableDeclaration;
import de.upb.hni.vmagic.expression.Aggregate;
import de.upb.hni.vmagic.expression.Expression;
import de.upb.hni.vmagic.expression.TypeConversion;
import de.upb.hni.vmagic.libraryunit.Entity;
import de.upb.hni.vmagic.literal.CharacterLiteral;
import de.upb.hni.vmagic.object.Constant;
import de.upb.hni.vmagic.object.Signal;
import de.upb.hni.vmagic.object.SignalAssignmentTarget;
import de.upb.hni.vmagic.object.Variable;
import de.upb.hni.vmagic.object.VariableAssignmentTarget;
import de.upb.hni.vmagic.object.VhdlObject;
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
    VHDLContext res = new VHDLContext();
    HDLVariable hVar = obj.toInterfaceRef().get().resolveVarForced("VHDL");
    res.merge(this.toVHDL(hVar.<HDLVariableDeclaration>getContainer(HDLVariableDeclaration.class), pid), false);
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
    int _xifexpression = (int) 0;
    if (process) {
      _xifexpression = res.newProcessID();
    } else {
      _xifexpression = pid;
    }
    final int newPid = _xifexpression;
    ArrayList<HDLStatement> _statements = obj.getStatements();
    for (final HDLStatement stmnt : _statements) {
      {
        final VHDLContext vhdl = this.toVHDL(stmnt, newPid);
        if ((vhdl == null)) {
          throw new HDLCodeGenerationException(stmnt, "No VHDL code could be generated", "VHDL");
        }
        res.merge(vhdl, false);
      }
    }
    return this.attachComment(res, obj);
  }
  
  public VHDLContext attachComment(final VHDLContext context, final IHDLObject block) {
    try {
      final SourceInfo srcInfo = block.<SourceInfo>getMeta(SourceInfo.INFO);
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
        if (((!newComments.isEmpty()) || (!docComments.isEmpty()))) {
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
      String _name = hEnum.getName();
      String _plus = ("$" + _name);
      String _plus_1 = (_plus + "_");
      String _name_1 = hVar.getName();
      String _plus_2 = (_plus_1 + _name_1);
      enums.add(VHDLUtils.getVHDLName(_plus_2));
    }
    final String[] enumArr = ((String[])Conversions.unwrapArray(enums, String.class));
    String _name_2 = hEnum.getName();
    String _plus_3 = ("$enum_" + _name_2);
    String _vHDLName = VHDLUtils.getVHDLName(_plus_3);
    EnumerationType _enumerationType = new EnumerationType(_vHDLName, enumArr);
    res.addTypeDeclaration(_enumerationType, false);
    return this.attachComment(res, obj);
  }
  
  protected VHDLContext _toVHDL(final HDLInterfaceDeclaration obj, final int pid) {
    return new VHDLContext();
  }
  
  private static EnumSet<HDLVariableDeclaration.HDLDirection> inAndOut = EnumSet.<HDLVariableDeclaration.HDLDirection>of(HDLVariableDeclaration.HDLDirection.IN, HDLVariableDeclaration.HDLDirection.INOUT, HDLVariableDeclaration.HDLDirection.OUT);
  
  protected VHDLContext _toVHDL(final HDLInterfaceInstantiation hii, final int pid) {
    final VHDLContext res = new VHDLContext();
    final HDLInterface hIf = hii.resolveHIfForced("VHDL");
    final HDLVariable interfaceVar = hii.getVar();
    final String ifName = hii.getVar().getName();
    final HDLQualifiedName asRef = hIf.asRef();
    final HDLInterfaceDeclaration hid = hIf.<HDLInterfaceDeclaration>getContainer(HDLInterfaceDeclaration.class);
    List<AssociationElement> portMap = null;
    List<AssociationElement> genericMap = null;
    ConcurrentStatement instantiation = null;
    final ArrayList<HDLVariableDeclaration> ports = hIf.getPorts();
    if (((hid != null) && (hid.getAnnotation(HDLBuiltInAnnotationProvider.HDLBuiltInAnnotations.VHDLComponent) != null))) {
      final HDLAnnotation anno = hid.getAnnotation(HDLBuiltInAnnotationProvider.HDLBuiltInAnnotations.VHDLComponent);
      String _value = null;
      if (anno!=null) {
        _value=anno.getValue();
      }
      boolean _equals = "declare".equals(_value);
      if (_equals) {
        String _string = asRef.getLastSegment().toString();
        final Component c = new Component(_string);
        final VHDLContext cContext = new VHDLContext();
        for (final HDLVariableDeclaration port : ports) {
          cContext.merge(this.toVHDL(port, (-1)), true);
        }
        for (final Signal signal : cContext.ports) {
          c.getPort().add(signal);
        }
        for (final ConstantDeclaration cd : cContext.constants) {
          List<Constant> _objects = cd.getObjects();
          for (final Object vobj : _objects) {
            c.getGeneric().add(((Constant) vobj));
          }
        }
        for (final Constant constant : cContext.generics) {
          c.getGeneric().add(constant);
        }
        res.addComponent(c);
      } else {
        res.addImport(VHDLPackageExtension.INST.getNameRef(asRef));
      }
      String _string_1 = asRef.getLastSegment().toString();
      final Component entity = new Component(_string_1);
      final ComponentInstantiation inst = new ComponentInstantiation(ifName, entity);
      portMap = inst.getPortMap();
      genericMap = inst.getGenericMap();
      instantiation = inst;
    } else {
      String _string_2 = VHDLPackageExtension.INST.getNameRef(asRef).toString();
      final Entity entity_1 = new Entity(_string_2);
      final EntityInstantiation inst_1 = new EntityInstantiation(ifName, entity_1);
      portMap = inst_1.getPortMap();
      genericMap = inst_1.getGenericMap();
      instantiation = inst_1;
    }
    HDLUnit unit = hii.<HDLUnit>getContainer(HDLUnit.class);
    HDLExport[] exportStmnts = unit.<HDLExport>getAllObjectsOf(HDLExport.class, true);
    final HDLExport[] _converted_exportStmnts = (HDLExport[])exportStmnts;
    final Function1<HDLExport, Boolean> _function = (HDLExport it) -> {
      HDLQualifiedName _varRefName = it.getVarRefName();
      return Boolean.valueOf((_varRefName != null));
    };
    final Function1<HDLExport, String> _function_1 = (HDLExport e) -> {
      return e.getVarRefName().getLastSegment();
    };
    Set<String> exportedSignals = IterableExtensions.<String>toSet(IterableExtensions.<HDLExport, String>map(IterableExtensions.<HDLExport>filter(((Iterable<HDLExport>)Conversions.doWrapArray(_converted_exportStmnts)), _function), _function_1));
    for (final HDLVariableDeclaration hvd : ports) {
      boolean _contains = VHDLStatementExtension.inAndOut.contains(hvd.getDirection());
      if (_contains) {
        this.generatePortMap(hvd, ifName, interfaceVar, asRef, res, hii, pid, portMap, exportedSignals);
      } else {
        HDLVariableDeclaration.HDLDirection _direction = hvd.getDirection();
        boolean _equals_1 = Objects.equal(_direction, HDLVariableDeclaration.HDLDirection.PARAMETER);
        if (_equals_1) {
          ArrayList<HDLVariable> _variables = hvd.getVariables();
          for (final HDLVariable hvar : _variables) {
            {
              HDLVariable sigVar = hvar;
              String _meta = hvar.<String>getMeta(HDLInterfaceInstantiation.ORIG_NAME);
              boolean _tripleNotEquals = (_meta != null);
              if (_tripleNotEquals) {
                sigVar = hvar.setName(hvar.<String>getMeta(HDLInterfaceInstantiation.ORIG_NAME));
              }
              final HDLVariableRef ref = hvar.asHDLRef();
              String _name = sigVar.getName();
              Expression _vHDL = this.vee.toVHDL(ref);
              AssociationElement _associationElement = new AssociationElement(_name, _vHDL);
              genericMap.add(_associationElement);
            }
          }
        }
      }
    }
    ForGenerateStatement forLoop = null;
    int _size = interfaceVar.getDimensions().size();
    boolean _equals_2 = (_size == 0);
    if (_equals_2) {
      res.addConcurrentStatement(instantiation);
    } else {
      int i = 0;
      ArrayList<HDLExpression> _dimensions = interfaceVar.getDimensions();
      for (final HDLExpression exp : _dimensions) {
        {
          final HDLExpression to = HDLArithOp.subtract(interfaceVar.getDimensions().get(i), 1);
          final HDLRange range = new HDLRange().setFrom(HDLLiteral.get(0)).setTo(to).setContainer(hii);
          String _asIndex = this.asIndex(Integer.valueOf(i));
          Range _vHDL = this.vee.toVHDL(range, Range.Direction.TO);
          final ForGenerateStatement newFor = new ForGenerateStatement(("generate_" + ifName), _asIndex, _vHDL);
          if ((forLoop != null)) {
            forLoop.getStatements().add(newFor);
          } else {
            res.addConcurrentStatement(newFor);
          }
          forLoop = newFor;
          i = (i + 1);
        }
      }
      if ((forLoop == null)) {
        throw new IllegalArgumentException("Should not get here");
      }
      forLoop.getStatements().add(instantiation);
    }
    return this.attachComment(res, hii);
  }
  
  public void generatePortMap(final HDLVariableDeclaration hvd, final String ifName, final HDLVariable interfaceVar, final HDLQualifiedName asRef, final VHDLContext res, final HDLInterfaceInstantiation obj, final int pid, final List<AssociationElement> portMap, final Set<String> exportedSignals) {
    final Collection<HDLAnnotation> typeAnno = HDLQuery.<HDLAnnotation>select(HDLAnnotation.class).from(hvd).<String>where(
      HDLAnnotation.fName).isEqualTo(HDLBuiltInAnnotationProvider.HDLBuiltInAnnotations.VHDLType.toString()).getAll();
    ArrayList<HDLVariable> _variables = hvd.getVariables();
    for (final HDLVariable hvar : _variables) {
      {
        HDLVariable sigVar = null;
        boolean _contains = exportedSignals.contains(hvar.getName());
        if (_contains) {
          sigVar = new HDLVariable().setName(hvar.getName());
          HDLVariableRef ref = sigVar.asHDLRef();
          String _vHDLName = VHDLUtils.getVHDLName(hvar.getName());
          Expression _vHDL = this.vee.toVHDL(ref);
          AssociationElement _associationElement = new AssociationElement(_vHDLName, _vHDL);
          portMap.add(_associationElement);
        } else {
          sigVar = hvar.setName(VHDLUtils.mapName(ifName, hvar.getName()));
          HDLVariableRef ref_1 = sigVar.asHDLRef();
          int i = 0;
          ArrayList<HDLExpression> _dimensions = interfaceVar.getDimensions();
          for (final HDLExpression exp : _dimensions) {
            {
              ref_1 = ref_1.addArray(new HDLVariableRef().setVar(HDLQualifiedName.create(this.asIndex(Integer.valueOf(i)))));
              i = (i + 1);
            }
          }
          ArrayList<HDLExpression> _dimensions_1 = interfaceVar.getDimensions();
          for (final HDLExpression exp_1 : _dimensions_1) {
            sigVar = sigVar.addDimensions(exp_1);
          }
          int _size = hvar.getDimensions().size();
          boolean _notEquals = (_size != 0);
          if (_notEquals) {
            boolean _isEmpty = typeAnno.isEmpty();
            if (_isEmpty) {
              final HDLQualifiedName name = VHDLPackageExtension.INST.getPackageNameRef(asRef).append(
                VHDLStatementExtension.getArrayRefName(hvar, true));
              res.addImport(name);
              final HDLVariableDeclaration newHVD = hvd.setDirection(HDLVariableDeclaration.HDLDirection.INTERNAL).setVariables(
                HDLObject.<HDLVariable>asList(
                  sigVar.setDimensions(null).addAnnotations(HDLBuiltInAnnotationProvider.HDLBuiltInAnnotations.VHDLType.create(name.toString())))).copyDeepFrozen(obj);
              res.merge(this.toVHDL(newHVD, pid), false);
            } else {
              final HDLVariableDeclaration newHVD_1 = hvd.setDirection(HDLVariableDeclaration.HDLDirection.INTERNAL).setVariables(
                HDLObject.<HDLVariable>asList(sigVar.setDimensions(null))).copyDeepFrozen(obj);
              res.merge(this.toVHDL(newHVD_1, pid), false);
            }
          } else {
            final HDLVariableDeclaration newHVD_2 = hvd.setDirection(HDLVariableDeclaration.HDLDirection.INTERNAL).setVariables(
              HDLObject.<HDLVariable>asList(sigVar)).copyDeepFrozen(obj);
            res.merge(this.toVHDL(newHVD_2, pid), false);
          }
          String _vHDLName_1 = VHDLUtils.getVHDLName(hvar.getName());
          Expression _vHDL_1 = this.vee.toVHDL(ref_1);
          AssociationElement _associationElement_1 = new AssociationElement(_vHDLName_1, _vHDL_1);
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
        fullName = hvar.<HDLQualifiedName>getMeta(VHDLStatementExtension.ORIGINAL_FULLNAME);
      } else {
        fullName = FullNameExtension.fullNameOf(hvar);
      }
      res = fullName.toString('_');
    } else {
      res = hvar.getName();
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
    final HDLAnnotation memAnno = HDLQuery.<HDLAnnotation>select(HDLAnnotation.class).from(obj).<String>where(HDLAnnotation.fName).isEqualTo(HDLBuiltInAnnotationProvider.HDLBuiltInAnnotations.memory.toString()).getFirst();
    if ((memAnno != null)) {
      return res;
    }
    final HDLAnnotation typeAnno = HDLQuery.<HDLAnnotation>select(HDLAnnotation.class).from(obj).<String>where(HDLAnnotation.fName).isEqualTo(HDLBuiltInAnnotationProvider.HDLBuiltInAnnotations.VHDLType.toString()).getFirst();
    HDLRegisterConfig _register = obj.getRegister();
    boolean _tripleNotEquals = (_register != null);
    if (_tripleNotEquals) {
      resetValue = obj.getRegister().getResetValue();
    }
    char _charAt = "0".charAt(0);
    CharacterLiteral _characterLiteral = new CharacterLiteral(_charAt);
    Expression<?> otherValue = Aggregate.OTHERS(_characterLiteral);
    if ((typeAnno != null)) {
      final String typeValue = typeAnno.getValue();
      boolean _endsWith = typeValue.endsWith("<>");
      if (_endsWith) {
        int _length = typeValue.length();
        int _minus = (_length - 2);
        String _substring = typeValue.substring(0, _minus);
        final HDLQualifiedName value = new HDLQualifiedName(_substring);
        res.addImport(value);
        String _lastSegment = value.getLastSegment();
        EnumerationType _enumerationType = new EnumerationType(_lastSegment);
        type = _enumerationType;
        HDLRange range = null;
        final HDLExpression width = primitive.getWidth();
        if ((width != null)) {
          range = new HDLRange().setFrom(HDLArithOp.subtract(width, 1)).setTo(HDLLiteral.get(0));
          range = range.copyDeepFrozen(obj);
          Range _vHDL = this.vee.toVHDL(range, Range.Direction.DOWNTO);
          IndexSubtypeIndication _indexSubtypeIndication = new IndexSubtypeIndication(type, _vHDL);
          type = _indexSubtypeIndication;
        }
      } else {
        final HDLQualifiedName value_1 = new HDLQualifiedName(typeValue);
        res.addImport(value_1);
        String _lastSegment_1 = value_1.getLastSegment();
        EnumerationType _enumerationType_1 = new EnumerationType(_lastSegment_1);
        type = _enumerationType_1;
      }
    } else {
      if ((primitive != null)) {
        type = VHDLCastsLibrary.getType(primitive);
      } else {
        final HDLType resolved = obj.resolveTypeForced("VHDL");
        if ((resolved instanceof HDLEnum)) {
          final HDLEnum hEnum = ((HDLEnum) resolved);
          String _name = hEnum.getName();
          String _plus = ("$enum_" + _name);
          String _vHDLName = VHDLUtils.getVHDLName(_plus);
          EnumerationType _enumerationType_2 = new EnumerationType(_vHDLName);
          type = _enumerationType_2;
          int idx = 0;
          HDLEvaluationContext _hDLEvaluationContext = new HDLEvaluationContext();
          final Procedure1<HDLEvaluationContext> _function = (HDLEvaluationContext it) -> {
            it.enumAsInt = true;
          };
          HDLEvaluationContext _doubleArrow = ObjectExtensions.<HDLEvaluationContext>operator_doubleArrow(_hDLEvaluationContext, _function);
          final Optional<BigInteger> resVal = ConstantEvaluate.valueOf(resetValue, _doubleArrow);
          boolean _isPresent = resVal.isPresent();
          if (_isPresent) {
            idx = resVal.get().intValue();
          }
          final HDLEnumRef enumReset = new HDLEnumRef().setHEnum(hEnum.asRef()).setVar(hEnum.getEnums().get(idx).asRef());
          enumReset.freeze(hEnum);
          otherValue = this.vee.toVHDL(enumReset);
          if ((!(resetValue instanceof HDLArrayInit))) {
            resetValue = enumReset;
          }
        }
      }
    }
    if ((type != null)) {
      ArrayList<HDLVariable> _variables = obj.getVariables();
      for (final HDLVariable hvar : _variables) {
        this.handleVariable(hvar, type, obj, res, resetValue, otherValue, pid);
      }
    }
    return this.attachComment(res, obj);
  }
  
  public void handleVariable(final HDLVariable hvar, final SubtypeIndication type, final HDLVariableDeclaration obj, final VHDLContext res, final HDLExpression resetValue, final Expression<?> otherValue, final int pid) {
    final boolean noExplicitResetVar = ((hvar.getAnnotation(HDLBuiltInAnnotationProvider.HDLBuiltInAnnotations.VHDLNoExplicitReset) != null) || (hvar.getAnnotation(HDLBuiltInAnnotationProvider.HDLBuiltInAnnotations.memory) != null));
    SubtypeIndication varType = type;
    int _size = hvar.getDimensions().size();
    boolean _notEquals = (_size != 0);
    if (_notEquals) {
      final LinkedList<DiscreteRange<?>> ranges = new LinkedList<DiscreteRange<?>>();
      ArrayList<HDLExpression> _dimensions = hvar.getDimensions();
      for (final HDLExpression arrayWidth : _dimensions) {
        {
          final HDLExpression newWidth = HDLArithOp.subtract(arrayWidth, 1);
          final Range range = this.vee.toVHDL(new HDLRange().setFrom(HDLLiteral.get(0)).setTo(newWidth).copyDeepFrozen(obj), Range.Direction.TO);
          ranges.add(range);
        }
      }
      final boolean external = obj.isExternal();
      String _arrayRefName = VHDLStatementExtension.getArrayRefName(hvar, external);
      final ConstrainedArray arrType = new ConstrainedArray(_arrayRefName, type, ((DiscreteRange[])Conversions.unwrapArray(ranges, DiscreteRange.class)));
      res.addTypeDeclaration(arrType, external);
      varType = arrType;
    }
    String name = hvar.getName();
    String _meta = hvar.<String>getMeta(HDLInterfaceInstantiation.ORIG_NAME);
    boolean _tripleNotEquals = (_meta != null);
    if (_tripleNotEquals) {
      name = hvar.<String>getMeta(HDLInterfaceInstantiation.ORIG_NAME);
    }
    String _name = hvar.getName();
    final Signal s = new Signal(_name, varType);
    if ((((resetValue != null) && (!noExplicitResetVar)) && (obj.getRegister() != null))) {
      boolean synchedArray = false;
      if ((resetValue instanceof HDLVariableRef)) {
        final HDLVariableRef ref = ((HDLVariableRef) resetValue);
        int _size_1 = ref.resolveVar().get().getDimensions().size();
        boolean _notEquals_1 = (_size_1 != 0);
        synchedArray = _notEquals_1;
      }
      final HDLVariableRef target = new HDLVariableRef().setVar(hvar.asRef());
      if ((resetValue instanceof HDLArrayInit)) {
        Expression _vHDLArray = this.vee.toVHDLArray(resetValue, otherValue);
        final SignalAssignment sa = new SignalAssignment(s, _vHDLArray);
        res.addResetValue(obj.getRegister(), sa);
      } else {
        final HDLStatement initLoop = Insulin.createArrayForLoop(Collections.<HDLExpression>emptyList(), hvar.getDimensions(), 0, resetValue, target, synchedArray).copyDeepFrozen(obj);
        final VHDLContext vhdl = this.toVHDL(initLoop, pid);
        res.addResetValue(obj.getRegister(), vhdl.getStatement());
      }
    }
    if (noExplicitResetVar) {
      if ((resetValue instanceof HDLArrayInit)) {
        s.setDefaultValue(this.vee.toVHDLArray(resetValue, otherValue));
      } else {
        if ((resetValue != null)) {
          Expression<?> assign = this.vee.toVHDL(resetValue);
          ArrayList<HDLExpression> _dimensions_1 = hvar.getDimensions();
          for (final HDLExpression exp : _dimensions_1) {
            assign = Aggregate.OTHERS(assign);
          }
          s.setDefaultValue(assign);
        }
      }
    }
    HDLVariableDeclaration.HDLDirection _direction = obj.getDirection();
    boolean _matched = false;
    if (Objects.equal(_direction, HDLVariableDeclaration.HDLDirection.IN)) {
      _matched=true;
      s.setMode(VhdlObject.Mode.IN);
      res.addPortDeclaration(s);
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
        HDLAnnotation _annotation = hvar.getAnnotation(HDLBuiltInAnnotationProvider.HDLBuiltInAnnotations.sharedVar);
        boolean _tripleNotEquals_1 = (_annotation != null);
        if (_tripleNotEquals_1) {
          String _name_1 = hvar.getName();
          Expression _defaultValue = s.getDefaultValue();
          final Variable sharedVar = new Variable(_name_1, varType, _defaultValue);
          HDLExpression _defaultValue_1 = hvar.getDefaultValue();
          boolean _tripleNotEquals_2 = (_defaultValue_1 != null);
          if (_tripleNotEquals_2) {
            sharedVar.setDefaultValue(this.vee.toVHDLArray(hvar.getDefaultValue(), otherValue));
          }
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
      if ((Objects.equal(obj.getDirection(), HDLVariableDeclaration.HDLDirection.HIDDEN) || Objects.equal(obj.getDirection(), HDLVariableDeclaration.HDLDirection.CONSTANT))) {
        _matched=true;
        final Constant constant = new Constant(name, varType);
        HDLExpression _defaultValue_2 = hvar.getDefaultValue();
        boolean _tripleNotEquals_3 = (_defaultValue_2 != null);
        if (_tripleNotEquals_3) {
          constant.setDefaultValue(this.vee.toVHDLArray(hvar.getDefaultValue(), otherValue));
        }
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
        final Constant constant_1 = new Constant(name, varType);
        HDLExpression _defaultValue_3 = hvar.getDefaultValue();
        boolean _tripleNotEquals_4 = (_defaultValue_3 != null);
        if (_tripleNotEquals_4) {
          constant_1.setDefaultValue(this.vee.toVHDLArray(hvar.getDefaultValue(), otherValue));
        }
        res.addGenericDeclaration(constant_1);
      }
    }
  }
  
  protected VHDLContext _toVHDL(final HDLSwitchStatement obj, final int pid) {
    final VHDLContext context = new VHDLContext();
    final HDLExpression hCaseExp = obj.getCaseExp();
    Optional<BigInteger> width = Optional.<BigInteger>absent();
    final Optional<? extends HDLType> type = TypeExtension.typeOf(hCaseExp);
    if ((type.isPresent() && (type.get() instanceof HDLPrimitive))) {
      HDLType _get = type.get();
      width = ConstantEvaluate.valueOf(((HDLPrimitive) _get).getWidth(), null);
      boolean _isPresent = width.isPresent();
      boolean _not = (!_isPresent);
      if (_not) {
        HDLType _get_1 = type.get();
        throw new HDLCodeGenerationException(_get_1, "Switch cases need a constant width", "VHDL");
      }
    }
    final Expression<?> caseExp = this.vee.toVHDL(hCaseExp);
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
        configs.addAll(vhdl.clockedStatements.keySet());
      }
    }
    for (final HDLRegisterConfig hdlRegisterConfig : configs) {
      {
        final CaseStatement cs_1 = new CaseStatement(caseExp);
        Set<Map.Entry<HDLSwitchCaseStatement, VHDLContext>> _entrySet = ctxs.entrySet();
        for (final Map.Entry<HDLSwitchCaseStatement, VHDLContext> e : _entrySet) {
          {
            final CaseStatement.Alternative alt = this.createAlternative(cs_1, e, width);
            final LinkedList<SequentialStatement> clockCase = e.getValue().clockedStatements.get(hdlRegisterConfig);
            if ((clockCase != null)) {
              alt.getStatements().addAll(clockCase);
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
          LinkedList<SequentialStatement> _get_2 = e.getValue().unclockedStatements.get(Integer.valueOf(pid));
          boolean _tripleNotEquals = (_get_2 != null);
          if (_tripleNotEquals) {
            alt.getStatements().addAll(e.getValue().unclockedStatements.get(Integer.valueOf(pid)));
          }
        }
      }
      context.addUnclockedStatement(pid, cs_1, obj);
    }
    return this.attachComment(context, obj);
  }
  
  private CaseStatement.Alternative createAlternative(final CaseStatement cs, final Map.Entry<HDLSwitchCaseStatement, VHDLContext> e, final Optional<BigInteger> bits) {
    CaseStatement.Alternative alt = null;
    final HDLExpression label = e.getKey().getLabel();
    if ((label != null)) {
      final Optional<BigInteger> eval = ConstantEvaluate.valueOf(label, null);
      boolean _isPresent = eval.isPresent();
      if (_isPresent) {
        boolean _isPresent_1 = bits.isPresent();
        boolean _not = (!_isPresent_1);
        if (_not) {
          throw new IllegalArgumentException("The width needs to be known for primitive types!");
        }
        alt = cs.createAlternative(VHDLUtils.toBinaryLiteral(bits.get().intValue(), eval.get()));
      } else {
        alt = cs.createAlternative(this.vee.toVHDL(label));
      }
    } else {
      alt = cs.createAlternative(Choices.OTHERS);
    }
    return alt;
  }
  
  protected VHDLContext _toVHDL(final HDLSwitchCaseStatement obj, final int pid) {
    final VHDLContext res = new VHDLContext();
    ArrayList<HDLStatement> _dos = obj.getDos();
    for (final HDLStatement stmnt : _dos) {
      res.merge(this.toVHDL(stmnt, pid), false);
    }
    return this.attachComment(res, obj);
  }
  
  protected VHDLContext _toVHDL(final HDLAssignment obj, final int pid) {
    final VHDLContext context = new VHDLContext();
    SequentialStatement sa = null;
    HDLReference ref = obj.getLeft();
    final HDLVariable hvar = ((HDLResolvedRef) ref).resolveVarForced("VHDL");
    final ArrayList<HDLExpression> dim = hvar.getDimensions();
    final Expression assTarget = this.vee.toVHDL(ref);
    Expression<?> value = this.vee.toVHDL(obj.getRight());
    if (((dim.size() != 0) && Objects.equal(ref.getClassType(), HDLClass.HDLVariableRef))) {
      final HDLVariableRef varRef = ((HDLVariableRef) ref);
      ArrayList<HDLExpression> _array = varRef.getArray();
      for (final HDLExpression exp : _array) {
        dim.remove(0);
      }
      if (((dim.size() != 0) && (!Objects.equal(obj.getRight().getClassType(), HDLClass.HDLArrayInit)))) {
        final HDLAnnotation typeAnno = hvar.getAnnotation(HDLBuiltInAnnotationProvider.HDLBuiltInAnnotations.VHDLType);
        if ((typeAnno != null)) {
          String _value = typeAnno.getValue();
          UnresolvedType _unresolvedType = new UnresolvedType(_value);
          Expression _vHDL = this.vee.toVHDL(obj.getRight());
          TypeConversion _typeConversion = new TypeConversion(_unresolvedType, _vHDL);
          value = _typeConversion;
        } else {
          final HDLVariableDeclaration hvd = hvar.<HDLVariableDeclaration>getContainer(HDLVariableDeclaration.class);
          String _arrayRefName = VHDLStatementExtension.getArrayRefName(hvar, hvd.isExternal());
          UnresolvedType _unresolvedType_1 = new UnresolvedType(_arrayRefName);
          Expression _vHDL_1 = this.vee.toVHDL(obj.getRight());
          TypeConversion _typeConversion_1 = new TypeConversion(_unresolvedType_1, _vHDL_1);
          value = _typeConversion_1;
        }
      }
    }
    HDLAnnotation _annotation = hvar.getAnnotation(HDLBuiltInAnnotationProvider.HDLBuiltInAnnotations.memory);
    boolean _tripleNotEquals = (_annotation != null);
    if (_tripleNotEquals) {
      VariableAssignment _variableAssignment = new VariableAssignment(((VariableAssignmentTarget) assTarget), value);
      sa = _variableAssignment;
    } else {
      SignalAssignment _signalAssignment = new SignalAssignment(((SignalAssignmentTarget) assTarget), value);
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
    ArrayList<HDLStatement> _dos = obj.getDos();
    for (final HDLStatement stmnt : _dos) {
      context.merge(this.toVHDL(stmnt, pid), false);
    }
    final VHDLContext res = new VHDLContext();
    res.merge(context, true);
    Set<Map.Entry<HDLRegisterConfig, LinkedList<SequentialStatement>>> _entrySet = context.clockedStatements.entrySet();
    for (final Map.Entry<HDLRegisterConfig, LinkedList<SequentialStatement>> e : _entrySet) {
      {
        String _vHDLName = VHDLUtils.getVHDLName(obj.getParam().getName());
        Range _vHDL = this.vee.toVHDL(obj.getRange().get(0), Range.Direction.TO);
        final ForStatement fStmnt = new ForStatement(_vHDLName, _vHDL);
        fStmnt.getStatements().addAll(e.getValue());
        res.addClockedStatement(e.getKey(), fStmnt);
      }
    }
    LinkedList<SequentialStatement> _get = context.unclockedStatements.get(Integer.valueOf(pid));
    boolean _tripleNotEquals = (_get != null);
    if (_tripleNotEquals) {
      String _vHDLName = VHDLUtils.getVHDLName(obj.getParam().getName());
      Range _vHDL = this.vee.toVHDL(obj.getRange().get(0), Range.Direction.TO);
      final ForStatement fStmnt = new ForStatement(_vHDLName, _vHDL);
      fStmnt.getStatements().addAll(context.unclockedStatements.get(Integer.valueOf(pid)));
      res.addUnclockedStatement(pid, fStmnt, obj);
    }
    return this.attachComment(res, obj);
  }
  
  protected VHDLContext _toVHDL(final HDLIfStatement obj, final int pid) {
    final VHDLContext thenCtx = new VHDLContext();
    ArrayList<HDLStatement> _thenDo = obj.getThenDo();
    for (final HDLStatement stmnt : _thenDo) {
      thenCtx.merge(this.toVHDL(stmnt, pid), false);
    }
    final VHDLContext elseCtx = new VHDLContext();
    ArrayList<HDLStatement> _elseDo = obj.getElseDo();
    for (final HDLStatement stmnt_1 : _elseDo) {
      elseCtx.merge(this.toVHDL(stmnt_1, pid), false);
    }
    final Set<HDLRegisterConfig> configs = new LinkedHashSet<HDLRegisterConfig>();
    configs.addAll(thenCtx.clockedStatements.keySet());
    configs.addAll(elseCtx.clockedStatements.keySet());
    final VHDLContext res = new VHDLContext();
    res.merge(thenCtx, true);
    res.merge(elseCtx, true);
    final Expression<?> ifExp = this.vee.toVHDL(obj.getIfExp());
    for (final HDLRegisterConfig config : configs) {
      {
        final IfStatement ifs = new IfStatement(ifExp);
        LinkedList<SequentialStatement> _get = thenCtx.clockedStatements.get(config);
        boolean _tripleNotEquals = (_get != null);
        if (_tripleNotEquals) {
          ifs.getStatements().addAll(thenCtx.clockedStatements.get(config));
        }
        LinkedList<SequentialStatement> _get_1 = elseCtx.clockedStatements.get(config);
        boolean _tripleNotEquals_1 = (_get_1 != null);
        if (_tripleNotEquals_1) {
          ifs.getElseStatements().addAll(elseCtx.clockedStatements.get(config));
        }
        res.addClockedStatement(config, ifs);
      }
    }
    if (((thenCtx.unclockedStatements.size() != 0) || (elseCtx.unclockedStatements.size() != 0))) {
      final IfStatement ifs = new IfStatement(ifExp);
      LinkedList<SequentialStatement> _get = thenCtx.unclockedStatements.get(Integer.valueOf(pid));
      boolean _tripleNotEquals = (_get != null);
      if (_tripleNotEquals) {
        ifs.getStatements().addAll(thenCtx.unclockedStatements.get(Integer.valueOf(pid)));
      }
      LinkedList<SequentialStatement> _get_1 = elseCtx.unclockedStatements.get(Integer.valueOf(pid));
      boolean _tripleNotEquals_1 = (_get_1 != null);
      if (_tripleNotEquals_1) {
        ifs.getElseStatements().addAll(elseCtx.unclockedStatements.get(Integer.valueOf(pid)));
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
