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
package org.pshdl.generator.vhdl;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.atomic.*;

import org.pshdl.model.*;
import org.pshdl.model.utils.*;

import com.google.common.collect.*;

import de.upb.hni.vmagic.*;
import de.upb.hni.vmagic.concurrent.*;
import de.upb.hni.vmagic.declaration.*;
import de.upb.hni.vmagic.object.*;
import de.upb.hni.vmagic.output.*;
import de.upb.hni.vmagic.statement.*;

public class VHDLContext {

	public Map<HDLRegisterConfig, LinkedList<SequentialStatement>> resetStatements = Maps.newLinkedHashMap();
	public Map<HDLRegisterConfig, LinkedList<SequentialStatement>> clockedStatements = Maps.newLinkedHashMap();
	public LinkedList<ConcurrentStatement> concurrentStatements = Lists.newLinkedList();
	public Map<Integer, LinkedList<SequentialStatement>> unclockedStatements = Maps.newLinkedHashMap();
	public Map<Integer, LinkedList<HDLStatement>> sensitiveStatements = Maps.newLinkedHashMap();
	public Map<Integer, Boolean> noSensitivity = Maps.newLinkedHashMap();
	public LinkedList<Signal> ports = Lists.newLinkedList();
	public LinkedList<ConstantDeclaration> constants = Lists.newLinkedList();
	public LinkedList<ConstantDeclaration> constantsPkg = Lists.newLinkedList();
	public LinkedList<Constant> generics = Lists.newLinkedList();
	public LinkedList<DeclarativeItem> internals = Lists.newLinkedList();
	public LinkedList<DeclarativeItemMarker> internalTypes = Lists.newLinkedList();
	public LinkedList<DeclarativeItemMarker> externalTypes = Lists.newLinkedList();
	public LinkedList<DeclarativeItemMarker> internalTypesConstants = Lists.newLinkedList();
	public Set<HDLQualifiedName> imports = Sets.newTreeSet();

	public void addClockedStatement(HDLRegisterConfig config, SequentialStatement sa) {
		config = config.normalize();
		LinkedList<SequentialStatement> list = clockedStatements.get(config);
		if (list == null) {
			list = Lists.newLinkedList();
		}
		list.add(sa);
		clockedStatements.put(config, list);
	}

	public void addUnclockedStatement(int pid, SequentialStatement sa, HDLStatement stmnt) {
		LinkedList<SequentialStatement> list = unclockedStatements.get(pid);
		if (list == null) {
			list = Lists.newLinkedList();
		}
		list.add(sa);
		unclockedStatements.put(pid, list);
		LinkedList<HDLStatement> hlist = sensitiveStatements.get(pid);
		if (hlist == null) {
			hlist = Lists.newLinkedList();
		}
		hlist.add(stmnt);
		sensitiveStatements.put(pid, hlist);
	}

	public static int DEFAULT_CTX = -1;

	public void merge(VHDLContext vhdl, boolean excludeStatements) {
		if (!excludeStatements) {
			concurrentStatements.addAll(vhdl.concurrentStatements);
			mergeListMap(vhdl, vhdl.sensitiveStatements, sensitiveStatements);
			mergeListMap(vhdl, vhdl.unclockedStatements, unclockedStatements);
			mergeListMap(vhdl, vhdl.clockedStatements, clockedStatements);
			mergeListMap(vhdl, vhdl.resetStatements, resetStatements);
		}
		ports.addAll(vhdl.ports);
		generics.addAll(vhdl.generics);
		constants.addAll(vhdl.constants);
		internalTypesConstants.addAll(vhdl.internalTypesConstants);
		constantsPkg.addAll(vhdl.constantsPkg);
		internals.addAll(vhdl.internals);
		internalTypes.addAll(vhdl.internalTypes);
		externalTypes.addAll(vhdl.externalTypes);
		imports.addAll(vhdl.imports);
		for (final Map.Entry<Integer, Boolean> entry : vhdl.noSensitivity.entrySet()) {
			noSensitivity.put(entry.getKey(), entry.getValue());
		}
	}

	private <K, T> void mergeListMap(VHDLContext vhdl, Map<K, LinkedList<T>> map, Map<K, LinkedList<T>> local) {
		for (final Entry<K, LinkedList<T>> e : map.entrySet()) {
			LinkedList<T> list = local.get(e.getKey());
			if (list == null) {
				list = Lists.newLinkedList();
				local.put(e.getKey(), list);
			}
			list.addAll(e.getValue());
		}
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		for (final Entry<HDLRegisterConfig, LinkedList<SequentialStatement>> e : clockedStatements.entrySet()) {
			printList(sb, e.getValue(), "For clock config " + e.getKey() + ":");
		}
		for (final Entry<HDLRegisterConfig, LinkedList<SequentialStatement>> e : resetStatements.entrySet()) {
			printList(sb, e.getValue(), "For clock config resets " + e.getKey() + ":");
		}
		printList(sb, concurrentStatements, "Concurrent Statements:");
		for (final Entry<Integer, LinkedList<SequentialStatement>> e : unclockedStatements.entrySet()) {
			printList(sb, e.getValue(), "For unclocked process " + e.getKey() + ":");
		}
		printList(sb, ports, "Entity ports:");
		printList(sb, generics, "Entity generics:");
		printList(sb, constants, "Entity constants:");
		printList(sb, constantsPkg, "Pkg constants:");
		printList(sb, internals, "Internal signals:");
		// printList(sb, internalTypes, "Internal types:");
		return sb.toString();
	}

	private void printList(StringBuilder sb, LinkedList<?> list, String label) {
		if (list.size() > 0) {
			sb.append(label).append("\n");
			for (final Object decl : list) {
				sb.append(VhdlOutput.toVhdlString((VhdlElement) decl)).append('\n');
			}
		}
	}

	public void addPortDeclaration(Signal sd) {
		ports.add(sd);
	}

	public void addInternalSignalDeclaration(ObjectDeclaration sd) {
		internals.add(sd);
	}

	public void addGenericDeclaration(Constant sd) {
		generics.add(sd);
	}

	public void addResetValue(HDLRegisterConfig config, SequentialStatement sa) {
		config = config.normalize();
		LinkedList<SequentialStatement> list = resetStatements.get(config);
		if (list == null) {
			list = Lists.newLinkedList();
		}
		list.add(sa);
		resetStatements.put(config, list);
	}

	public SequentialStatement getStatement() {
		if ((clockedStatements.size() > 1) || (unclockedStatements.size() > 1))
			throw new IllegalArgumentException("Did not expect to find more than one statement:" + this);
		for (final LinkedList<SequentialStatement> clkd : clockedStatements.values()) {
			if (clkd.size() > 1)
				throw new IllegalArgumentException("Did not expect to find more than one statement:" + this);
			return clkd.getFirst();
		}
		for (final LinkedList<SequentialStatement> clkd : unclockedStatements.values()) {
			if (clkd.size() > 1)
				throw new IllegalArgumentException("Did not expect to find more than one statement:" + this);
			return clkd.getFirst();
		}
		throw new NoSuchElementException("No Statement found");
	}

	public void addConstantDeclaration(ConstantDeclaration cd) {
		constants.add(cd);
		internalTypesConstants.add(cd);
	}

	public void addTypeDeclaration(DeclarativeItemMarker type, boolean isExternal) {
		if (isExternal) {
			externalTypes.add(type);
		} else {
			internalTypes.add(type);
			internalTypesConstants.add(type);
		}
	}

	public boolean hasPkgDeclarations() {
		return (externalTypes.size() != 0) || (constantsPkg.size() != 0);
	}

	public void addConcurrentStatement(ConcurrentStatement stmnt) {
		concurrentStatements.add(stmnt);
	}

	public void addImport(HDLQualifiedName value) {
		imports.add(value.skipLast(1));
	}

	public void addConstantDeclarationPkg(ConstantDeclaration cd) {
		constantsPkg.add(cd);
	}

	private static AtomicInteger ai = new AtomicInteger();

	/**
	 * Generates a new process id
	 * 
	 * @return an unused process id
	 */
	public int newProcessID() {
		return ai.incrementAndGet();
	}

	/**
	 * Marks a process as containing no sensitivity. This is useful for
	 * simulation purposes where wait functions are used
	 * 
	 * @param pid
	 *            the process id
	 */
	public void setNoSensitivity(int pid) {
		noSensitivity.put(pid, true);
	}

	public void addComponent(Component c) {
		internals.add(c);
	}

}
