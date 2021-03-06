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

import de.upb.hni.vmagic.AssociationElement
import de.upb.hni.vmagic.VhdlElement
import de.upb.hni.vmagic.VhdlFile
import de.upb.hni.vmagic.builtin.NumericStd
import de.upb.hni.vmagic.builtin.StdLogic1164
import de.upb.hni.vmagic.concurrent.ProcessStatement
import de.upb.hni.vmagic.declaration.ConstantDeclaration
import de.upb.hni.vmagic.expression.Equals
import de.upb.hni.vmagic.expression.FunctionCall
import de.upb.hni.vmagic.libraryunit.Architecture
import de.upb.hni.vmagic.libraryunit.Entity
import de.upb.hni.vmagic.libraryunit.LibraryClause
import de.upb.hni.vmagic.libraryunit.LibraryUnit
import de.upb.hni.vmagic.libraryunit.PackageDeclaration
import de.upb.hni.vmagic.libraryunit.UseClause
import de.upb.hni.vmagic.literal.EnumerationLiteral
import de.upb.hni.vmagic.object.Constant
import de.upb.hni.vmagic.object.ConstantGroup
import de.upb.hni.vmagic.object.Signal
import de.upb.hni.vmagic.object.SignalGroup
import de.upb.hni.vmagic.statement.IfStatement
import de.upb.hni.vmagic.statement.IfStatement.ElsifPart
import de.upb.hni.vmagic.statement.SequentialStatement
import de.upb.hni.vmagic.statement.WaitStatement
import de.upb.hni.vmagic.type.Type
import de.upb.hni.vmagic.type.UnresolvedType
import de.upb.hni.vmagic.util.Comments
import java.util.ArrayList
import java.util.Collection
import java.util.Collections
import java.util.EnumSet
import java.util.LinkedHashSet
import java.util.LinkedList
import java.util.List
import java.util.Map
import java.util.Set
import java.util.TreeSet
import org.pshdl.generator.vhdl.libraries.VHDLCastsLibrary
import org.pshdl.generator.vhdl.libraries.VHDLShiftLibrary
import org.pshdl.generator.vhdl.libraries.VHDLTypesLibrary
import org.pshdl.model.HDLAssignment
import org.pshdl.model.HDLClass
import org.pshdl.model.HDLDeclaration
import org.pshdl.model.HDLEnumDeclaration
import org.pshdl.model.HDLEnumRef
import org.pshdl.model.HDLPackage
import org.pshdl.model.HDLReference
import org.pshdl.model.HDLRegisterConfig
import org.pshdl.model.HDLResolvedRef
import org.pshdl.model.HDLStatement
import org.pshdl.model.HDLUnit
import org.pshdl.model.HDLUnresolvedFragment
import org.pshdl.model.HDLVariable
import org.pshdl.model.HDLVariableDeclaration
import org.pshdl.model.HDLVariableDeclaration.HDLDirection
import org.pshdl.model.HDLVariableRef
import org.pshdl.model.IHDLObject
import org.pshdl.model.parser.SourceInfo
import org.pshdl.model.utils.HDLQualifiedName
import org.pshdl.model.utils.ModificationSet
import org.pshdl.model.utils.Refactoring

import static org.pshdl.model.extensions.FullNameExtension.*
import de.upb.hni.vmagic.statement.SequentialStatementVisitor
import de.upb.hni.vmagic.statement.CaseStatement
import org.pshdl.model.types.builtIn.HDLBuiltInAnnotationProvider.HDLBuiltInAnnotations

class WaitSeacher extends SequentialStatementVisitor {
	public boolean hasWait = false

	override protected visitWaitStatement(WaitStatement statement) {
		hasWait = true
	}
}

class VHDLPackageExtension {

	extension VHDLExpressionExtension vee = new VHDLExpressionExtension
	extension VHDLStatementExtension vse = new VHDLStatementExtension

	public static VHDLPackageExtension INST = new VHDLPackageExtension

	def List<LibraryUnit> toVHDL(HDLUnit obj) {
		val List<LibraryUnit> res = new LinkedList<LibraryUnit>
		val HDLQualifiedName entityName = fullNameOf(obj)
		val Entity e = new Entity(entityName.dashString)

		val VHDLContext unit = new VHDLContext

		val HDLEnumRef[] hRefs = obj.getAllObjectsOf(typeof(HDLEnumRef), true)
		for (HDLEnumRef hdlEnumRef : hRefs) {
			val resolveHEnum = hdlEnumRef.resolveHEnum
			val HDLUnit enumContainer = resolveHEnum.get.getContainer(typeof(HDLUnit))
			if (enumContainer === null || !enumContainer.equals(hdlEnumRef.getContainer(typeof(HDLUnit)))) {
				val HDLQualifiedName type = fullNameOf(resolveHEnum.get)
				if (!type.getSegment(0).equals("pshdl"))
					unit.addImport(HDLQualifiedName.create("work", getPackageName(type), "all"))
			}
		}
		val HDLVariableRef[] vRefs = obj.getAllObjectsOf(typeof(HDLVariableRef), true)
		for (HDLVariableRef variableRef : vRefs) {
			if (variableRef.classType != HDLClass.HDLInterfaceRef) {
				val variable = variableRef.resolveVarForced("VHDL")
				val HDLUnit enumContainer = variable.getContainer(typeof(HDLUnit))
				if (enumContainer === null || !enumContainer.equals(variableRef.getContainer(typeof(HDLUnit)))) {
					val HDLQualifiedName type = fullNameOf(variable).skipLast(1)
					if (type.length > 0 && !type.getSegment(0).equals("pshdl"))
						unit.addImport(HDLQualifiedName.create("work", getPackageName(type), "all"))
				}
			}
		}

		for (HDLStatement stmnt : obj.inits) {
			val vhdl = stmnt.toVHDL(VHDLContext.DEFAULT_CTX)
			if (vhdl !== null)
				unit.merge(vhdl, false)
			else
				print("Failed to translate: " + stmnt)
		}
		for (HDLStatement stmnt : obj.statements) {
			val vhdl = stmnt.toVHDL(VHDLContext.DEFAULT_CTX)
			if (vhdl !== null)
				unit.merge(vhdl, false)
			else
				print("Failed to translate: " + stmnt)
		}
		addDefaultLibs(res, unit)
		if (unit.hasPkgDeclarations) {
			val String libName = getPackageName(entityName)
			val PackageDeclaration pd = new PackageDeclaration(libName)
			pd.declarations.addAll(unit.externalTypes as List)
			pd.declarations.addAll(unit.constantsPkg)
			res.add(pd)
			res.add(new UseClause('''work.«libName».all'''))
			addDefaultLibs(res, unit)
		}
		for (Signal sig : unit.ports) {
			val comments = Comments.getComments(sig)
			val sg = new SignalGroup(sig)
			Comments.setComments(sg, comments)
			e.port.add(sg)
		}
		for (Constant sig : unit.generics) {
			val comments = Comments.getComments(sig)
			val sg = new ConstantGroup(sig)
			Comments.setComments(sg, comments)
			e.generic.add(sg)
		}
		e.declarations.addAll(unit.internalTypesConstants as List)
		res.add(e)
		val Architecture a = new Architecture("pshdlGenerated", e)
		e.attachComments(obj, true, false)
		a.attachComments(obj, false, true)
		a.declarations.addAll(unit.components.values as Collection)
		a.declarations.addAll(unit.internals as List)
		a.statements.addAll(unit.concurrentStatements)
		for (Map.Entry<Integer,LinkedList<SequentialStatement>> uc : unit.unclockedStatements.entrySet) {
			val ProcessStatement ps = new ProcessStatement
			ps.sensitivityList.addAll(createSensitivyList(unit, uc.key))
			ps.statements.addAll(uc.value)
			if (ps.sensitivityList.empty) {
				val WaitSeacher ssv = new WaitSeacher
				ps.statements.forEach[ssv.visit(it)]
				if (!ssv.hasWait)
					ps.statements.add(new WaitStatement)
			}
			a.statements.add(ps)
		}
		for (Map.Entry<HDLRegisterConfig, LinkedList<SequentialStatement>> pc : unit.clockedStatements.entrySet) {
			val ProcessStatement ps = new ProcessStatement
			ps.statements.add(createIfStatement(obj, ps, pc.key, pc.value, unit))
			a.statements.add(ps)
		}
		res.add(a)
		return res
	}

	def attachComments(VhdlElement e, IHDLObject obj, boolean doc, boolean normal) {
		val srcInfo = obj.getMeta(SourceInfo.INFO)
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
			if (doc && normal)
				Comments.setComments(e, newComments + docComments)
			else if (doc)
				Comments.setComments(e, docComments)
			else if (normal)
				Comments.setComments(e, newComments)
		}
	}

	def public String getPackageName(HDLQualifiedName entityName) '''«entityName.dashString»Pkg'''

	def dashString(HDLQualifiedName name) {
		return name.toString('_'.charAt(0))
	}

	def public HDLQualifiedName getPackageNameRef(HDLQualifiedName entityName) {
		if (entityName.getSegment(0).equals("VHDL"))
			return entityName.skipFirst(1)
		return HDLQualifiedName.create("work", '''«entityName.dashString»Pkg''')
	}

	def public HDLQualifiedName getNameRef(HDLQualifiedName entityName) {
		if (entityName.getSegment(0).equals("VHDL"))
			return entityName.skipFirst(1)
		return HDLQualifiedName.create("work", entityName.dashString)
	}

	def private static addDefaultLibs(List<LibraryUnit> res, VHDLContext unit) {
		val usedLibs = staticImports(res)
		for (HDLQualifiedName i : unit.imports) {
			val String lib = i.getSegment(0)
			if (!usedLibs.contains(lib)) {
				res.add(new LibraryClause(lib))
				usedLibs.add(lib)
			}
			res.add(new UseClause(i.append("all").toString))
		}
	}

	def static staticImports(List<LibraryUnit> res) {
		res.add(new LibraryClause("ieee"))
		res.add(StdLogic1164.USE_CLAUSE)
		res.add(NumericStd.USE_CLAUSE)

		// res.add(new LibraryClause("pshdl"))
		res.add(VHDLCastsLibrary.USE_CLAUSE)
		res.add(VHDLShiftLibrary.USE_CLAUSE)
		res.add(VHDLTypesLibrary.USE_CLAUSE)
		val Set<String> usedLibs = new LinkedHashSet<String>

		// usedLibs.add("pshdl")
		usedLibs.add("ieee")
		usedLibs.add("work")
		return usedLibs
	}

	private static EnumSet<HDLDirection> notSensitive = EnumSet.of(HDLDirection.HIDDEN, HDLDirection.PARAMETER,
		HDLDirection.CONSTANT)

	def private Collection<? extends Signal> createSensitivyList(VHDLContext ctx, int pid) {
		if (ctx.noSensitivity.containsKey(pid))
			return Collections.emptyList
		val List<Signal> sensitivity = new LinkedList<Signal>
		val Set<String> vars = new TreeSet<String>
		for (HDLStatement stmnt : ctx.sensitiveStatements.get(pid)) {
			val HDLVariableRef[] refs = stmnt.getAllObjectsOf(typeof(HDLVariableRef), true)
			for (HDLVariableRef ref : refs) {
				val hvar = ref.resolveVarForced("VHDL")
				//Memories generate a variable, those don't need to be in the sensitivity list
				if (hvar.getAnnotation(HDLBuiltInAnnotations.memory) === null) {
					val IHDLObject container = hvar.container
					if (container instanceof HDLVariableDeclaration) {
						val HDLVariableDeclaration hdv = container as HDLVariableDeclaration
						if (!notSensitive.contains(hdv.direction)) {
							if (ref.container instanceof HDLAssignment) {
								val HDLAssignment hAss = ref.container as HDLAssignment
								if (hAss.left.resolveVar.registerConfig !== null) {
								} else if (hAss.left != ref) {
									vars.add(ref.VHDLName)
								}
							} else {
								vars.add(ref.VHDLName)
							}
						}
					}
				}
			}
		}

		for (String string : vars) {
			sensitivity.add(new Signal(string, UnresolvedType.NO_NAME))
		}
		return sensitivity
	}

	def HDLVariable resolveVar(HDLReference reference) {
		if (reference instanceof HDLUnresolvedFragment)
			throw new RuntimeException("Can not use unresolved fragments")
		return (reference as HDLResolvedRef).resolveVar.get
	}

	def private SequentialStatement createIfStatement(HDLUnit hUnit, ProcessStatement ps, HDLRegisterConfig config,
		LinkedList<SequentialStatement> value, VHDLContext unit) {
		val key = config.normalize
		var Signal clk = key.clk.toVHDL as Signal
		var Signal rst = key.rst.toVHDL as Signal
		ps.sensitivityList.add(clk)
		var EnumerationLiteral activeRst
		if (key.resetType === HDLRegisterConfig.HDLRegResetActiveType.HIGH)
			activeRst = StdLogic1164.STD_LOGIC_1
		else
			activeRst = StdLogic1164.STD_LOGIC_0
		var IfStatement rstIfStmnt = new IfStatement(new Equals(rst, activeRst))
		val LinkedList<SequentialStatement> resets = unit.resetStatements.get(key)
		if (resets !== null)
			rstIfStmnt.statements.addAll(resets)
		var FunctionCall edge
		if (key.clockType === HDLRegisterConfig.HDLRegClockType.RISING)
			edge = new FunctionCall(StdLogic1164.RISING_EDGE)
		else
			edge = new FunctionCall(StdLogic1164.FALLING_EDGE)
		edge.parameters.add(new AssociationElement(clk))
		if (key.syncType === HDLRegisterConfig.HDLRegSyncType.ASYNC) {
			ps.sensitivityList.add(rst)
			val ElsifPart elsifPart = rstIfStmnt.createElsifPart(edge)
			elsifPart.statements.addAll(value)
			return rstIfStmnt
		}
		val IfStatement clkIf = new IfStatement(edge)
		clkIf.statements.add(rstIfStmnt)
		rstIfStmnt.elseStatements.addAll(value)
		return clkIf
	}

	def VhdlFile toVHDL(HDLPackage obj) {
		val VhdlFile res = new VhdlFile
		var PackageDeclaration pd = null
		for (HDLDeclaration decl : obj.declarations) {
			if (decl.classType == HDLClass.HDLVariableDeclaration) {
				val HDLVariableDeclaration hvd = decl as HDLVariableDeclaration
				if (pd === null) {
					staticImports(res.elements)
					pd = new PackageDeclaration(getPackageName(new HDLQualifiedName(obj.pkg)))
					res.elements.add(pd)
				}
				val VHDLContext vhdl = hvd.toVHDL(VHDLContext.DEFAULT_CTX)
				var ConstantDeclaration first = if(vhdl.constants.empty) null else vhdl.constants.first
				if (first === null) {
					first = vhdl.constantsPkg.first
					if (first === null)
						throw new IllegalArgumentException("Expected constant declaration but found none!")
				}
				pd.declarations.add(first)
			}
			if (decl.classType == HDLClass.HDLEnumDeclaration) {
				val HDLEnumDeclaration hvd = decl as HDLEnumDeclaration
				val PackageDeclaration enumPd = new PackageDeclaration(getPackageName(fullNameOf(hvd.HEnum)))
				res.elements.add(enumPd)
				val VHDLContext vhdl = hvd.toVHDL(VHDLContext.DEFAULT_CTX)
				val Type first = vhdl.internalTypes.first as Type
				if (first === null)
					throw new IllegalArgumentException("Expected enum type declaration but found none!")
				enumPd.declarations.add(first)
			}
		}
		for (HDLUnit unit : obj.units) {
			val ModificationSet ms = new ModificationSet
			val HDLVariableDeclaration[] hvds = unit.getAllObjectsOf(typeof(HDLVariableDeclaration), true)
			for (HDLVariableDeclaration hvd : hvds) {
				for (HDLVariable hvar : hvd.variables) {
					val HDLVariableRef[] refs = hvar.getAllObjectsOf(typeof(HDLVariableRef), true)
					for (HDLVariableRef ref : refs) {

						// Check which variable declaration contains references and mark those references as the ones that should be declared in a package
						val resolvedRef = ref.resolveVar
						// References for types imported from other modules might not be visible in the HDL Model
						if (resolvedRef.present)
							resolvedRef.get.setMeta(VHDLStatementExtension.EXPORT)
					}
					val String origName = hvar.name
					val String name = VHDLUtils.getVHDLName(origName)
					if (name != origName)
						Refactoring.renameVariable(hvar, hvar.asRef().skipLast(1).append(name), unit, ms)
				}
			}
			val HDLUnit newUnit = ms.apply(unit)
			res.elements.addAll(newUnit.toVHDL)
		}
		return res
	}
}
