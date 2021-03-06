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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.concurrent.ExecutorService;

import org.antlr.runtime.tree.RewriteCardinalityException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.pshdl.generator.vhdl.VHDLOutputValidator.VHDLErrorCode;
import org.pshdl.model.HDLClass;
import org.pshdl.model.HDLInterface;
import org.pshdl.model.HDLPackage;
import org.pshdl.model.HDLUnresolvedFragment;
import org.pshdl.model.utils.HDLCodeGenerationException;
import org.pshdl.model.utils.HDLLibrary;
import org.pshdl.model.utils.HDLProblemException;
import org.pshdl.model.utils.HDLQualifiedName;
import org.pshdl.model.utils.Insulin;
import org.pshdl.model.utils.PSAbstractCompiler;
import org.pshdl.model.utils.services.IOutputProvider;
import org.pshdl.model.validation.Problem;

import com.google.common.collect.Lists;

import de.upb.hni.vmagic.output.VhdlOutput;

/**
 * This compiler is the central place for generating VHDL output and auxiliary
 * files for PSHDL. The basic operation is like this:
 *
 * <ol>
 * <li>{@link #setup(String, ExecutorService)}</li>
 * <li>{@link #add(File)} Add as many files as you want. You can also add VHDL
 * files with {@link #addVHDL(PSAbstractCompiler, File)}</li>
 * <li>{@link PSAbstractCompiler#compile(ICompilationListener)} generates all
 * VHDL code and auxiliary files</li>
 * </ol>
 *
 * @author Karsten Becker
 *
 */
public class PStoVHDLCompiler extends PSAbstractCompiler implements IOutputProvider {

	private static final String HOOK_NAME = "vhdl";

	public PStoVHDLCompiler() {
		this(null, null);
	}

	public PStoVHDLCompiler(String uri, ExecutorService service) {
		super(uri, service);
	}

	@Override
	public CompileResult doCompile(final String src, final HDLPackage parse) {
		final HDLPackage transform = Insulin.transform(parse, src);
		final HDLUnresolvedFragment[] allObjectsOf = (HDLUnresolvedFragment[]) transform.getAllObjectsOf(HDLClass.HDLUnresolvedFragment.clazz, true);
		if (allObjectsOf.length != 0)
			throw new HDLCodeGenerationException(allObjectsOf[0], "Some elements failed to resolve in the preparation", "VHDL");
		final String vhdlCode = VhdlOutput.toVhdlString(VHDLPackageExtension.INST.toVHDL(transform));
		return createResult(src, vhdlCode, getHookName(), false);
	}

	/**
	 * This is the command line version of the compiler
	 *
	 * @param cli
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	@Override
	public String invoke(CommandLine cli) throws IOException {
		final List<String> argList = cli.getArgList();
		if (argList.size() == 0) {
			getUsage().printHelp(System.out);
			return "Missing file arguments";
		}
		final File outDir = getOutputDir(cli);
		final List<File> pshdlFiles = Lists.newArrayListWithCapacity(argList.size());
		for (final String string : argList) {
			final File file = new File(string);
			if (!file.exists())
				return "File: " + file + " does not exist";
			if (string.endsWith(".vhdl") || string.endsWith(".vhd")) {
				final List<HDLInterface> vhdl = addVHDL(this, file);
				if (cli.hasOption('i') && (vhdl != null)) {
					final File ifFile = new File(outDir, file.getName() + ".pshdl");
					final PrintStream ps = new PrintStream(ifFile, "UTF-8");
					for (final HDLInterface hdlInterface : vhdl) {
						ps.println(hdlInterface);
					}
					ps.close();
				}
			}
			if (string.endsWith(".pshdl")) {
				pshdlFiles.add(file);
			}
		}
		try {
			if (addFiles(pshdlFiles)) {
				printErrors();
				return "Found syntax errors";
			}
			validatePackages();
			printErrors();
		} catch (final Exception e1) {
			e1.printStackTrace();
			return "An exception occured during file parsing, this should not happen";
		}
		System.out.println("Compiling files");
		final SimpleListener simpleListener = new SimpleListener();
		List<CompileResult> results;
		try {
			results = compile(simpleListener);
		} catch (final Exception e) {
			e.printStackTrace();
			return "An exception occured during file parsing, this should not happen";
		}
		for (final CompileResult result : results) {
			if (!result.hasError()) {
				writeFiles(outDir, result);
			} else {
				System.out.println("Failed to generate code for:" + result.src);
			}
		}
		return null;
	}

	public static File getOutputDir(CommandLine cli) {
		final File outDir = new File(cli.getOptionValue('o', "src-gen"));
		if (!outDir.exists() && !outDir.mkdirs())
			throw new IllegalArgumentException("Failed to create directory:" + outDir);
		return outDir;
	}

	/**
	 * Adds a VHDL file to the {@link HDLLibrary} so that interfaces can be
	 * resolved
	 *
	 * @param comp
	 *            the compiler to which the files should be added
	 *
	 * @param file
	 *            the VHDL file
	 * @return the generated interfaces of the VHDL file
	 * @throws IOException
	 */
	public static List<HDLInterface> addVHDL(PSAbstractCompiler comp, File file) throws IOException {
		try (final FileInputStream fis = new FileInputStream(file)) {
			return addVHDL(comp, fis, file.getAbsolutePath());
		}
	}

	/**
	 * Imports the given stream as HDLInterface. This allows it to be
	 * referenced. The generated interface can be found in package VHDL.work
	 *
	 * @param comp
	 *
	 * @param contents
	 *            the contents of the VHDL file
	 * @param asSrc
	 *            a src id under which to register the {@link HDLInterface}
	 * @return the generated interfaces of the VHDL file
	 */
	public static List<HDLInterface> addVHDL(PSAbstractCompiler comp, InputStream contents, String asSrc) {
		comp.invalidate();
		final HDLLibrary lib = HDLLibrary.getLibrary(comp.uri);
		List<HDLInterface> importFile = null;
		try {
			importFile = VHDLImporter.importFile(HDLQualifiedName.create("VHDL", "work"), contents, lib, asSrc);
			comp.clearError(asSrc);
		} catch (final IOException | RewriteCardinalityException e) {
			comp.addError(asSrc, new Problem(VHDLErrorCode.PARSE_ERROR, e.getMessage(), 0, 0, 1, 0));
		} catch (final HDLProblemException e) {
			for (final Problem p : e.problems) {
				comp.addError(asSrc, p);
			}
		}
		return importFile;
	}

	@Override
	public String getHookName() {
		return HOOK_NAME;
	}

	@Override
	public MultiOption getUsage() {
		return getMultiOptions();
	}

	public static MultiOption getMultiOptions() {
		final Options options = new Options();
		options.addOption(new Option("o", "outputDir", true, "Specify the directory to which the files will be written, default is: src-gen"));
		options.addOption(new Option("i", "interface", false, "Generate pshdl interface declarations for vhdl file arguments"));
		return new MultiOption(HOOK_NAME + " usage: [OPTIONS] <files>", null, options);
	}

	public static PStoVHDLCompiler setup(String uri, ExecutorService service) {
		return new PStoVHDLCompiler(uri, service);
	}

}
