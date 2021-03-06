/*******************************************************************************
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
 ******************************************************************************/
package org.pshdl.generator.vhdl

import com.google.common.base.Optional
import de.upb.hni.vmagic.AssociationElement
import de.upb.hni.vmagic.Choices
import de.upb.hni.vmagic.DiscreteRange
import de.upb.hni.vmagic.Range
import de.upb.hni.vmagic.Range.Direction
import de.upb.hni.vmagic.concurrent.ComponentInstantiation
import de.upb.hni.vmagic.concurrent.ConcurrentStatement
import de.upb.hni.vmagic.concurrent.EntityInstantiation
import de.upb.hni.vmagic.concurrent.ForGenerateStatement
import de.upb.hni.vmagic.declaration.Component
import de.upb.hni.vmagic.declaration.ConstantDeclaration
import de.upb.hni.vmagic.declaration.SignalDeclaration
import de.upb.hni.vmagic.declaration.VariableDeclaration
import de.upb.hni.vmagic.expression.Aggregate
import de.upb.hni.vmagic.expression.Expression
import de.upb.hni.vmagic.expression.TypeConversion
import de.upb.hni.vmagic.libraryunit.Entity
import de.upb.hni.vmagic.literal.CharacterLiteral
import de.upb.hni.vmagic.object.Constant
import de.upb.hni.vmagic.object.Signal
import de.upb.hni.vmagic.object.SignalAssignmentTarget
import de.upb.hni.vmagic.object.Variable
import de.upb.hni.vmagic.object.VariableAssignmentTarget
import de.upb.hni.vmagic.object.VhdlObject
import de.upb.hni.vmagic.statement.CaseStatement
import de.upb.hni.vmagic.statement.CaseStatement.Alternative
import de.upb.hni.vmagic.statement.ForStatement
import de.upb.hni.vmagic.statement.IfStatement
import de.upb.hni.vmagic.statement.SequentialStatement
import de.upb.hni.vmagic.statement.SignalAssignment
import de.upb.hni.vmagic.statement.VariableAssignment
import de.upb.hni.vmagic.type.ConstrainedArray
import de.upb.hni.vmagic.type.EnumerationType
import de.upb.hni.vmagic.type.IndexSubtypeIndication
import de.upb.hni.vmagic.type.SubtypeIndication
import de.upb.hni.vmagic.type.UnresolvedType
import java.math.BigInteger
import java.util.ArrayList
import java.util.Collection
import java.util.Collections
import java.util.EnumSet
import java.util.LinkedHashMap
import java.util.LinkedHashSet
import java.util.LinkedList
import java.util.List
import java.util.Map
import java.util.Set
import org.pshdl.generator.vhdl.libraries.VHDLCastsLibrary
import org.pshdl.model.HDLAnnotation
import org.pshdl.model.HDLArithOp
import org.pshdl.model.HDLArrayInit
import org.pshdl.model.HDLAssignment
import org.pshdl.model.HDLBlock
import org.pshdl.model.HDLClass
import org.pshdl.model.HDLDirectGeneration
import org.pshdl.model.HDLEnum
import org.pshdl.model.HDLEnumDeclaration
import org.pshdl.model.HDLEnumRef
import org.pshdl.model.HDLExport
import org.pshdl.model.HDLExpression
import org.pshdl.model.HDLForLoop
import org.pshdl.model.HDLFunction
import org.pshdl.model.HDLFunctionCall
import org.pshdl.model.HDLIfStatement
import org.pshdl.model.HDLInterface
import org.pshdl.model.HDLInterfaceDeclaration
import org.pshdl.model.HDLInterfaceInstantiation
import org.pshdl.model.HDLLiteral
import org.pshdl.model.HDLObject
import org.pshdl.model.HDLObject.GenericMeta
import org.pshdl.model.HDLPrimitive
import org.pshdl.model.HDLRange
import org.pshdl.model.HDLReference
import org.pshdl.model.HDLRegisterConfig
import org.pshdl.model.HDLResolvedRef
import org.pshdl.model.HDLStatement
import org.pshdl.model.HDLSwitchCaseStatement
import org.pshdl.model.HDLSwitchStatement
import org.pshdl.model.HDLUnit
import org.pshdl.model.HDLVariable
import org.pshdl.model.HDLVariableDeclaration
import org.pshdl.model.HDLVariableDeclaration.HDLDirection
import org.pshdl.model.HDLVariableRef
import org.pshdl.model.IHDLObject
import org.pshdl.model.evaluation.ConstantEvaluate
import org.pshdl.model.extensions.FullNameExtension
import org.pshdl.model.extensions.TypeExtension
import org.pshdl.model.parser.SourceInfo
import org.pshdl.model.types.builtIn.HDLBuiltInAnnotationProvider.HDLBuiltInAnnotations
import org.pshdl.model.utils.HDLCodeGenerationException
import org.pshdl.model.utils.HDLQualifiedName
import org.pshdl.model.utils.HDLQuery
import org.pshdl.model.utils.Insulin

import static org.pshdl.model.types.builtIn.HDLBuiltInAnnotationProvider.HDLBuiltInAnnotations.*
import org.pshdl.model.evaluation.HDLEvaluationContext
import org.pshdl.model.HDLManip

class VHDLStatementExtension {
	public static VHDLStatementExtension INST = new VHDLStatementExtension

	def static vhdlOf(HDLStatement stmnt, int pid) {
		return INST.toVHDL(stmnt, pid)
	}

	extension VHDLExpressionExtension vee = new VHDLExpressionExtension

	private static GenericMeta<HDLQualifiedName> ORIGINAL_FULLNAME = new GenericMeta<HDLQualifiedName>(
		"ORIGINAL_FULLNAME", true);

	public static GenericMeta<Boolean> EXPORT = new GenericMeta<Boolean>("EXPORT", true)

	def dispatch VHDLContext toVHDL(IHDLObject obj, int pid) {
		throw new IllegalArgumentException("Not correctly implemented:" + obj.classType + " " + obj)
	}

	def dispatch VHDLContext toVHDL(HDLExport obj, int pid) {
		var res = new VHDLContext
		var hVar = obj.toInterfaceRef.get.resolveVarForced("VHDL")
		res.merge(hVar.getContainer(HDLVariableDeclaration).toVHDL(pid), false)
		return res
	}

	def dispatch VHDLContext toVHDL(HDLDirectGeneration obj, int pid) {
		return new VHDLContext
	}

	def dispatch VHDLContext toVHDL(HDLFunctionCall obj, int pid) {
		return VHDLFunctions.toOutputStatement(obj, pid, null)
	}

	def dispatch VHDLContext toVHDL(HDLBlock obj, int pid) {
		val VHDLContext res = new VHDLContext
		var boolean process = false
		if (obj.process !== null && obj.process) {
			process = true
		}
		val newPid = if(process) res.newProcessID else pid
		for (HDLStatement stmnt : obj.statements) {
			val vhdl = stmnt.toVHDL(newPid)
			if (vhdl === null)
				throw new HDLCodeGenerationException(stmnt, "No VHDL code could be generated", "VHDL")
			res.merge(vhdl, false)
		}
		return res.attachComment(obj)
	}

	def VHDLContext attachComment(VHDLContext context, IHDLObject block) {
		try {
			val srcInfo = block.getMeta(SourceInfo.INFO)
			if (srcInfo !== null) {
				val newComments = new ArrayList<String>
				val docComments = new ArrayList<String>
				for (String comment : srcInfo.comments) {
					if (comment.startsWith("//")) {
						val newComment = comment.substring(2, comment.length - 1)
						if (newComment.startsWith("/")) {
							if (newComment.startsWith("/<"))
								docComments.add(newComment.substring(2))
							else
								docComments.add(newComment.substring(1))
						} else
							newComments.add(newComment)
					} else {
						val newComment = comment.substring(2, comment.length - 2)
						if (newComment.startsWith("*")) {
							if (newComment.startsWith("*<"))
								docComments.addAll(newComment.substring(2).split("\n"))
							else
								docComments.addAll(newComment.substring(1).split("\n"))
						} else
							newComments.addAll(newComment.split("\n"))
					}
				}
				if (!newComments.empty || !docComments.empty)
					context.attachComments(newComments, docComments)
			}
		} catch (Exception e) {
		}
		return context
	}

	def dispatch VHDLContext toVHDL(HDLEnumDeclaration obj, int pid) {
		val VHDLContext res = new VHDLContext
		val HDLEnum hEnum = obj.HEnum
		val List<String> enums = new LinkedList<String>
		for (HDLVariable hVar : hEnum.enums) {
			enums.add(VHDLUtils.getVHDLName("$" + hEnum.name + "_" + hVar.name))
		}
		val String[] enumArr = enums
		res.addTypeDeclaration(new EnumerationType(VHDLUtils.getVHDLName("$enum_" + hEnum.name), enumArr), false)
		return res.attachComment(obj)
	}

	def dispatch VHDLContext toVHDL(HDLInterfaceDeclaration obj, int pid) {
		return new VHDLContext
	}

	private static EnumSet<HDLDirection> inAndOut = EnumSet.of(HDLDirection.IN, HDLDirection.INOUT, HDLDirection.OUT)

	def dispatch VHDLContext toVHDL(HDLInterfaceInstantiation hii, int pid) {
		val VHDLContext res = new VHDLContext
		val HDLInterface hIf = hii.resolveHIfForced("VHDL")
		val HDLVariable interfaceVar = hii.^var
		val String ifName = hii.^var.name
		val HDLQualifiedName asRef = hIf.asRef
		val HDLInterfaceDeclaration hid = hIf.getContainer(HDLInterfaceDeclaration)
		var List<AssociationElement> portMap
		var List<AssociationElement> genericMap
		var ConcurrentStatement instantiation

		// Perform instantiation as Component rather than Entity if
		// VHDLComponent Annotation is present
		val ArrayList<HDLVariableDeclaration> ports = hIf.ports
		if (hid !== null && hid.getAnnotation(VHDLComponent) !== null) {
			val HDLAnnotation anno = hid.getAnnotation(VHDLComponent)
			if ("declare".equals(anno?.value)) {
				val Component c = new Component(asRef.lastSegment.toString)
				val VHDLContext cContext = new VHDLContext
				for (HDLVariableDeclaration port : ports) {
					cContext.merge(port.toVHDL(-1), true)
				}
				for (Signal signal : cContext.ports) {
					c.port.add(signal)
				}
				for (ConstantDeclaration cd : cContext.constants) {
					for (Object vobj : cd.objects)
						c.generic.add(vobj as Constant)
				}
				for (Constant constant : cContext.generics) {
					c.generic.add(constant)
				}
				res.addComponent(c)
			} else
				res.addImport(VHDLPackageExtension.INST.getNameRef(asRef))
			val Component entity = new Component(asRef.lastSegment.toString)
			val ComponentInstantiation inst = new ComponentInstantiation(ifName, entity)
			portMap = inst.portMap
			genericMap = inst.genericMap
			instantiation = inst
		} else {
			val Entity entity = new Entity(VHDLPackageExtension.INST.getNameRef(asRef).toString)
			val EntityInstantiation inst = new EntityInstantiation(ifName, entity)
			portMap = inst.portMap
			genericMap = inst.genericMap
			instantiation = inst
		}
		var unit = hii.getContainer(HDLUnit)
		var exportStmnts = unit.getAllObjectsOf(HDLExport, true)
		var exportedSignals = exportStmnts.filter[it.varRefName !== null].map[e|e.varRefName.lastSegment].toSet
		for (HDLVariableDeclaration hvd : ports) {
			if (inAndOut.contains(hvd.direction)) {
				generatePortMap(hvd, ifName, interfaceVar, asRef, res, hii, pid, portMap, exportedSignals)
			} else {

				// Parameter get a special treatment because they have been renamed by HDLInterfaceInstantiation resolveIF
				if (hvd.direction == HDLDirection.PARAMETER) {
					for (HDLVariable hvar : hvd.variables) {
						var HDLVariable sigVar = hvar
						if (hvar.getMeta(HDLInterfaceInstantiation.ORIG_NAME) !== null)
							sigVar = hvar.setName(hvar.getMeta(HDLInterfaceInstantiation.ORIG_NAME))

						val HDLVariableRef ref = hvar.asHDLRef
						genericMap.add(new AssociationElement(sigVar.name, ref.toVHDL))
					}
				}
			}
		}
		var ForGenerateStatement forLoop = null
		if (interfaceVar.dimensions.size == 0)
			res.addConcurrentStatement(instantiation)
		else {
			var i = 0;
			for (HDLExpression exp : interfaceVar.dimensions) {
				val HDLExpression to = HDLArithOp.subtract(interfaceVar.dimensions.get(i), 1)
				val HDLRange range = new HDLRange().setFrom(HDLLiteral.get(0)).setTo(to).setContainer(hii)
				val ForGenerateStatement newFor = new ForGenerateStatement("generate_" + ifName, i.asIndex,
					range.toVHDL(Range.Direction.TO))
				if (forLoop !== null)
					forLoop.statements.add(newFor)
				else
					res.addConcurrentStatement(newFor)
				forLoop = newFor
				i = i + 1;
			}
			if (forLoop === null)
				throw new IllegalArgumentException("Should not get here")
			forLoop.statements.add(instantiation)
		}
		return res.attachComment(hii)
	}

	def generatePortMap(HDLVariableDeclaration hvd, String ifName, HDLVariable interfaceVar, HDLQualifiedName asRef,
		VHDLContext res, HDLInterfaceInstantiation obj, int pid, List<AssociationElement> portMap,
		Set<String> exportedSignals) {
			val Collection<HDLAnnotation> typeAnno = HDLQuery.select(typeof(HDLAnnotation)).from(hvd).where(
				HDLAnnotation.fName).isEqualTo(VHDLType.toString).all
			for (HDLVariable hvar : hvd.variables) {
				var HDLVariable sigVar
				if (exportedSignals.contains(hvar.name)) {
					sigVar = new HDLVariable().setName(hvar.name)
					var HDLVariableRef ref = sigVar.asHDLRef
					portMap.add(new AssociationElement(VHDLUtils.getVHDLName(hvar.name), ref.toVHDL))
				} else {
					sigVar = hvar.setName(VHDLUtils.mapName(ifName, hvar.name))
					var HDLVariableRef ref = sigVar.asHDLRef
					var i = 0
					for (HDLExpression exp : interfaceVar.dimensions) {
						ref = ref.addArray(new HDLVariableRef().setVar(HDLQualifiedName.create(i.asIndex)))
						i = i + 1;
					}
					for (HDLExpression exp : interfaceVar.dimensions) {
						sigVar = sigVar.addDimensions(exp)
					}
					if (hvar.dimensions.size != 0) {

						// Arrays are always named in VHDL, so the type annotation should be present
						if (typeAnno.isEmpty) {
							val HDLQualifiedName name = VHDLPackageExtension.INST.getPackageNameRef(asRef).append(
								getArrayRefName(hvar, true))
							res.addImport(name)
							val HDLVariableDeclaration newHVD = hvd.setDirection(HDLDirection.INTERNAL).setVariables(
								HDLObject.asList(
									sigVar.setDimensions(null).addAnnotations(VHDLType.create(name.toString)))).
								copyDeepFrozen(obj)
							res.merge(newHVD.toVHDL(pid), false)
						} else {
							val HDLVariableDeclaration newHVD = hvd.setDirection(HDLDirection.INTERNAL).setVariables(
								HDLObject.asList(sigVar.setDimensions(null))).copyDeepFrozen(obj)
							res.merge(newHVD.toVHDL(pid), false)
						}
					} else {
						val HDLVariableDeclaration newHVD = hvd.setDirection(HDLDirection.INTERNAL).setVariables(
							HDLObject.asList(sigVar)).copyDeepFrozen(obj)
						res.merge(newHVD.toVHDL(pid), false)
					}
					portMap.add(new AssociationElement(VHDLUtils.getVHDLName(hvar.name), ref.toVHDL))
				}
			}
		}

		def String asIndex(Integer integer) {
			val int i = 'I'.charAt(0)
			return Character.toString((i + integer) as char);
		}

		def static String getArrayRefName(HDLVariable hvar, boolean external) {
			var String res
			if (external) {
				var HDLQualifiedName fullName
				if (hvar.getMeta(ORIGINAL_FULLNAME) !== null)
					fullName = hvar.getMeta(ORIGINAL_FULLNAME)
				else
					fullName = FullNameExtension.fullNameOf(hvar)
				res = fullName.toString('_')
			} else
				res = hvar.name
			return VHDLUtils.getVHDLName(VHDLUtils.unescapeVHDLName(res) + "_array")
		}

		def dispatch VHDLContext toVHDL(HDLVariableDeclaration obj, int pid) {
			val VHDLContext res = new VHDLContext
			val HDLPrimitive primitive = obj.primitive
			var SubtypeIndication type = null
			var HDLExpression resetValue = null
			val HDLAnnotation memAnno = HDLQuery.select(typeof(HDLAnnotation)).from(obj).where(HDLAnnotation.fName).
				isEqualTo(memory.toString).first
			if (memAnno !== null)
				return res
			val HDLAnnotation typeAnno = HDLQuery.select(typeof(HDLAnnotation)).from(obj).where(HDLAnnotation.fName).
				isEqualTo(VHDLType.toString).first
			if (obj.register !== null) {
				resetValue = obj.register.resetValue
			}
			var Expression<?> otherValue = Aggregate.OTHERS(new CharacterLiteral('0'.charAt(0)))
			if (typeAnno !== null) {
				val typeValue = typeAnno.value
				if (typeValue.endsWith("<>")) {
					val HDLQualifiedName value = new HDLQualifiedName(typeValue.substring(0, typeValue.length - 2))
					res.addImport(value)
					type = new EnumerationType(value.lastSegment)
					var HDLRange range = null;
					val width = primitive.width
					if (width !== null) {
						range = new HDLRange().setFrom(HDLArithOp.subtract(width, 1)).setTo(HDLLiteral.get(0));
						range = range.copyDeepFrozen(obj);
						type = new IndexSubtypeIndication(type, range.toVHDL(Direction.DOWNTO))
					}
				} else {
					val HDLQualifiedName value = new HDLQualifiedName(typeValue)
					res.addImport(value)
					type = new EnumerationType(value.lastSegment)
				}
			} else {
				if (primitive !== null) {
					type = VHDLCastsLibrary.getType(primitive)
				} else {
					val resolved = obj.resolveTypeForced("VHDL")
					if (resolved instanceof HDLEnum) {
						val HDLEnum hEnum = resolved as HDLEnum
						type = new EnumerationType(VHDLUtils.getVHDLName("$enum_" + hEnum.name))
						var idx = 0;
						val resVal = ConstantEvaluate.valueOf(resetValue,
							new HDLEvaluationContext => [enumAsInt = true])
						if (resVal.present)
							idx = resVal.get.intValue
						val enumReset = new HDLEnumRef().setHEnum(hEnum.asRef).setVar(hEnum.enums.get(idx).asRef)
						enumReset.freeze(hEnum)
						otherValue = enumReset.toVHDL
						if (!(resetValue instanceof HDLArrayInit))
							resetValue = enumReset
					}
				}
			}
			if (type !== null) {
				for (HDLVariable hvar : obj.variables) {
					handleVariable(hvar, type, obj, res, resetValue, otherValue, pid)
				}
			}
			return res.attachComment(obj)
		}

		def handleVariable(HDLVariable hvar, SubtypeIndication type, HDLVariableDeclaration obj, VHDLContext res,
			HDLExpression resetValue, Expression<?> otherValue, int pid) {
			val boolean noExplicitResetVar = (hvar.getAnnotation(VHDLNoExplicitReset) !== null) ||
				(hvar.getAnnotation(memory) !== null)
			var SubtypeIndication varType = type
			if (hvar.dimensions.size != 0) {
				val ranges = new LinkedList<DiscreteRange<?>>
				for (HDLExpression arrayWidth : hvar.dimensions) {
					val HDLExpression newWidth = HDLArithOp.subtract(arrayWidth, 1)
					val Range range = new HDLRange().setFrom(HDLLiteral.get(0)).setTo(newWidth).copyDeepFrozen(obj).
						toVHDL(Range.Direction.TO)
					ranges.add(range)
				}
				val boolean external = obj.isExternal
				val ConstrainedArray arrType = new ConstrainedArray(getArrayRefName(hvar, external), type, ranges)
				res.addTypeDeclaration(arrType, external)
				varType = arrType
			}
			var name = hvar.name
			if (hvar.getMeta(HDLInterfaceInstantiation.ORIG_NAME) !== null) {
				name = hvar.getMeta(HDLInterfaceInstantiation.ORIG_NAME)
			}
			val Signal s = new Signal(hvar.name, varType)
			if (resetValue !== null && !noExplicitResetVar && obj.register !== null) {
				var boolean synchedArray = false
				if (resetValue instanceof HDLVariableRef) {
					val HDLVariableRef ref = resetValue as HDLVariableRef
					synchedArray = ref.resolveVar.get.dimensions.size != 0
				}
				val target = new HDLVariableRef().setVar(hvar.asRef)
				if (resetValue instanceof HDLArrayInit) {
					val sa = new SignalAssignment(s, resetValue.toVHDLArray(otherValue))
					res.addResetValue(obj.register, sa)
				} else {
					val HDLStatement initLoop = Insulin.createArrayForLoop(Collections.emptyList, hvar.dimensions, 0,
						resetValue, target, synchedArray).copyDeepFrozen(obj)
					val VHDLContext vhdl = initLoop.toVHDL(pid)
					res.addResetValue(obj.register, vhdl.statement)
				}
			}
			if (noExplicitResetVar) {
				if (resetValue instanceof HDLArrayInit) {
					s.setDefaultValue(resetValue.toVHDLArray(otherValue))
				} else {
					if (resetValue !== null) {
						var Expression<?> assign
						if (resetValue instanceof HDLLiteral && obj.primitive!==null){
							assign = HDLManip.getCast(obj.primitive, resetValue).toVHDL
						} else{
							assign = resetValue.toVHDL
						}
						for (HDLExpression exp : hvar.dimensions)
							assign = Aggregate.OTHERS(assign)
						s.setDefaultValue(assign)
					}
				}
			}
			switch (obj.direction) {
				case IN: {
					s.setMode(VhdlObject.Mode.IN)
					res.addPortDeclaration(s)
				}
				case OUT: {
					s.setMode(VhdlObject.Mode.OUT)
					res.addPortDeclaration(s)
				}
				case INOUT: {
					s.setMode(VhdlObject.Mode.INOUT)
					res.addPortDeclaration(s)
				}
				case INTERNAL: {
					if (hvar.getAnnotation(HDLBuiltInAnnotations.sharedVar) !== null) {
						val sharedVar = new Variable(hvar.name, varType, s.defaultValue)
						if (hvar.defaultValue !== null)
							sharedVar.setDefaultValue(hvar.defaultValue.toVHDLArray(otherValue))
						sharedVar.shared = true
						val vd = new VariableDeclaration(sharedVar)
						res.addInternalSignalDeclaration(vd)
					} else {
						val SignalDeclaration sd = new SignalDeclaration(s)
						res.addInternalSignalDeclaration(sd)
					}
				}
				case obj.direction == HIDDEN || obj.direction == CONSTANT: {
					val Constant constant = new Constant(name, varType)
					if (hvar.defaultValue !== null)
						constant.setDefaultValue(hvar.defaultValue.toVHDLArray(otherValue))

					val ConstantDeclaration cd = new ConstantDeclaration(constant)
					if (hvar.hasMeta(EXPORT))
						res.addConstantDeclarationPkg(cd)
					else
						res.addConstantDeclaration(cd)
				}
				case PARAMETER: {
					val Constant constant = new Constant(name, varType)
					if (hvar.defaultValue !== null)
						constant.setDefaultValue(hvar.defaultValue.toVHDLArray(otherValue))
					res.addGenericDeclaration(constant)
				}
			}
		}

		def dispatch VHDLContext toVHDL(HDLSwitchStatement obj, int pid) {
			val VHDLContext context = new VHDLContext
			val HDLExpression hCaseExp = obj.caseExp
			var Optional<BigInteger> width = Optional.absent
			val type = TypeExtension.typeOf(hCaseExp)
			if (type.present && type.get instanceof HDLPrimitive) {
				width = ConstantEvaluate.valueOf((type.get as HDLPrimitive).width, null)
				if (!width.present)
					throw new HDLCodeGenerationException(type.get, "Switch cases need a constant width", "VHDL")
			}
			val Expression<?> caseExp = hCaseExp.toVHDL
			val Map<HDLSwitchCaseStatement, VHDLContext> ctxs = new LinkedHashMap<HDLSwitchCaseStatement, VHDLContext>
			val Set<HDLRegisterConfig> configs = new LinkedHashSet<HDLRegisterConfig>
			var boolean hasUnclocked = false
			for (HDLSwitchCaseStatement cs : obj.cases) {
				val VHDLContext vhdl = cs.toVHDL(pid)
				ctxs.put(cs, vhdl)
				if (vhdl.unclockedStatements.size > 0)
					hasUnclocked = true
				configs.addAll(vhdl.clockedStatements.keySet)
			}
			for (HDLRegisterConfig hdlRegisterConfig : configs) {
				val CaseStatement cs = new CaseStatement(caseExp)
				for (Map.Entry<HDLSwitchCaseStatement, VHDLContext> e : ctxs.entrySet) {
					val Alternative alt = createAlternative(cs, e, width)
					val LinkedList<SequentialStatement> clockCase = e.value.clockedStatements.get(hdlRegisterConfig)
					if (clockCase !== null) {
						alt.statements.addAll(clockCase)
					}
				}
				context.addClockedStatement(hdlRegisterConfig, cs)
			}
			if (hasUnclocked) {
				val CaseStatement cs = new CaseStatement(caseExp)
				for (Map.Entry<HDLSwitchCaseStatement, VHDLContext> e : ctxs.entrySet) {
					val Alternative alt = createAlternative(cs, e, width)
					if (e.value.unclockedStatements.get(pid) !== null)
						alt.statements.addAll(e.value.unclockedStatements.get(pid))
				}
				context.addUnclockedStatement(pid, cs, obj)
			}
			return context.attachComment(obj)
		}

		def private Alternative createAlternative(CaseStatement cs, Map.Entry<HDLSwitchCaseStatement, VHDLContext> e,
			Optional<BigInteger> bits) {
			var Alternative alt
			val HDLExpression label = e.key.label
			if (label !== null) {
				val Optional<BigInteger> eval = ConstantEvaluate.valueOf(label, null)
				if (eval.present) {
					if (!bits.present)
						throw new IllegalArgumentException("The width needs to be known for primitive types!")
					alt = cs.createAlternative(VHDLUtils.toBinaryLiteral(bits.get.intValue, eval.get))
				} else
					alt = cs.createAlternative(label.toVHDL); // can only be an enum
			} else {
				alt = cs.createAlternative(Choices.OTHERS)
			}
			return alt
		}

		def dispatch VHDLContext toVHDL(HDLSwitchCaseStatement obj, int pid) {
			val VHDLContext res = new VHDLContext
			for (HDLStatement stmnt : obj.dos) {
				res.merge(stmnt.toVHDL(pid), false)
			}
			return res.attachComment(obj)
		}

		def dispatch VHDLContext toVHDL(HDLAssignment obj, int pid) {
			val VHDLContext context = new VHDLContext
			var SequentialStatement sa = null
			var HDLReference ref = obj.left
			val hvar = (ref as HDLResolvedRef).resolveVarForced("VHDL")
			val ArrayList<HDLExpression> dim = hvar.dimensions
			val assTarget = ref.toVHDL
			var Expression<?> value = obj.right.toVHDL
			if (dim.size != 0 && ref.classType == HDLClass.HDLVariableRef) {
				val HDLVariableRef varRef = ref as HDLVariableRef
				for (HDLExpression exp : varRef.array) {
					dim.remove(0)
				}
				if (dim.size != 0 && obj.right.classType != HDLClass.HDLArrayInit) {
					// XXX Implement correct array assignment for non full assignments
					val HDLAnnotation typeAnno = hvar.getAnnotation(VHDLType)
					if (typeAnno !== null) {
						value = new TypeConversion(new UnresolvedType(typeAnno.value), obj.right.toVHDL)
					} else {
						val HDLVariableDeclaration hvd = hvar.getContainer(typeof(HDLVariableDeclaration))
						value = new TypeConversion(new UnresolvedType(getArrayRefName(hvar, hvd.isExternal)),
							obj.right.toVHDL)
					}
				}
			}
			if (hvar.getAnnotation(HDLBuiltInAnnotations.memory) !== null)
				sa = new VariableAssignment(assTarget as VariableAssignmentTarget, value)
			else
				sa = new SignalAssignment(assTarget as SignalAssignmentTarget, value)
			val HDLRegisterConfig config = hvar.registerConfig
			if (config !== null)
				context.addClockedStatement(config, sa)
			else
				context.addUnclockedStatement(pid, sa, obj)
			return context.attachComment(obj)
		}

		def dispatch VHDLContext toVHDL(HDLForLoop obj, int pid) {
			val VHDLContext context = new VHDLContext
			for (HDLStatement stmnt : obj.dos) {
				context.merge(stmnt.toVHDL(pid), false)
			}
			val VHDLContext res = new VHDLContext
			res.merge(context, true)
			for (Map.Entry<HDLRegisterConfig, LinkedList<SequentialStatement>> e : context.clockedStatements.entrySet) {
				val ForStatement fStmnt = new ForStatement(VHDLUtils.getVHDLName(obj.param.name),
					obj.range.get(0).toVHDL(Range.Direction.TO))
				fStmnt.statements.addAll(e.value)
				res.addClockedStatement(e.key, fStmnt)
			}
			if (context.unclockedStatements.get(pid) !== null) {
				val ForStatement fStmnt = new ForStatement(VHDLUtils.getVHDLName(obj.param.name),
					obj.range.get(0).toVHDL(Range.Direction.TO))
				fStmnt.statements.addAll(context.unclockedStatements.get(pid))
				res.addUnclockedStatement(pid, fStmnt, obj)
			}
			return res.attachComment(obj)
		}

		def dispatch VHDLContext toVHDL(HDLIfStatement obj, int pid) {
			val VHDLContext thenCtx = new VHDLContext
			for (HDLStatement stmnt : obj.thenDo) {
				thenCtx.merge(stmnt.toVHDL(pid), false)
			}
			val VHDLContext elseCtx = new VHDLContext
			for (HDLStatement stmnt : obj.elseDo) {
				elseCtx.merge(stmnt.toVHDL(pid), false)
			}
			val Set<HDLRegisterConfig> configs = new LinkedHashSet<HDLRegisterConfig>
			configs.addAll(thenCtx.clockedStatements.keySet)
			configs.addAll(elseCtx.clockedStatements.keySet)
			val VHDLContext res = new VHDLContext
			res.merge(thenCtx, true)
			res.merge(elseCtx, true)
			val Expression<?> ifExp = obj.ifExp.toVHDL
			for (HDLRegisterConfig config : configs) {
				val IfStatement ifs = new IfStatement(ifExp)
				if (thenCtx.clockedStatements.get(config) !== null)
					ifs.statements.addAll(thenCtx.clockedStatements.get(config))
				if (elseCtx.clockedStatements.get(config) !== null)
					ifs.elseStatements.addAll(elseCtx.clockedStatements.get(config))
				res.addClockedStatement(config, ifs)
			}
			if (thenCtx.unclockedStatements.size != 0 || elseCtx.unclockedStatements.size != 0) {
				val IfStatement ifs = new IfStatement(ifExp)
				if (thenCtx.unclockedStatements.get(pid) !== null)
					ifs.statements.addAll(thenCtx.unclockedStatements.get(pid))
				if (elseCtx.unclockedStatements.get(pid) !== null)
					ifs.elseStatements.addAll(elseCtx.unclockedStatements.get(pid))
				res.addUnclockedStatement(pid, ifs, obj)
			}
			return res.attachComment(obj)
		}

		def dispatch VHDLContext toVHDL(HDLFunction obj, int pid) {
			throw new IllegalArgumentException("Not supported")
		}
	}
	