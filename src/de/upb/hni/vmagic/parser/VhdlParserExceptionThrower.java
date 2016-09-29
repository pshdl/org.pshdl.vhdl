/*******************************************************************************
 * PSHDL is a library and (trans-)compiler for PSHDL input. It generates
 *     output suitable for implementation or simulation of it.
 *
 *     Copyright (C) 2014 Karsten Becker (feedback (at) pshdl (dot) org)
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
/*
 * Copyright 2009, 2010 University of Paderborn
 *
 * This file is part of vMAGIC parser.
 *
 * vMAGIC is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * vMAGIC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with vMAGIC. If not, see <http://www.gnu.org/licenses/>.
 *
 * Authors: Ralf Fuest <rfuest@users.sourceforge.net>
 *          Christopher Pohl <cpohl@users.sourceforge.net>
 */
package de.upb.hni.vmagic.parser;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import org.antlr.runtime.CharStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.Token;
import org.antlr.runtime.TokenStream;
import org.antlr.runtime.tree.CommonTreeAdaptor;
import org.antlr.runtime.tree.CommonTreeNodeStream;

import com.google.common.collect.Lists;

import de.upb.hni.vmagic.Annotations;
import de.upb.hni.vmagic.LibraryDeclarativeRegion;
import de.upb.hni.vmagic.RootDeclarativeRegion;
import de.upb.hni.vmagic.VhdlFile;
import de.upb.hni.vmagic.parser.annotation.ParseErrors;
import de.upb.hni.vmagic.parser.annotation.PositionInformation;
import de.upb.hni.vmagic.parser.annotation.SourcePosition;
import de.upb.hni.vmagic.parser.antlr.MetaClassCreator;
import de.upb.hni.vmagic.parser.antlr.VhdlAntlrLexer;
import de.upb.hni.vmagic.parser.antlr.VhdlAntlrParser;
import de.upb.hni.vmagic.parser.util.CaseInsensitiveInputStream;

/**
 * VHDL parser.
 */
public class VhdlParserExceptionThrower {

	/**
	 * Prevent instantiation.
	 */
	private VhdlParserExceptionThrower() {
	}

	private static VhdlFile parse(VhdlParserSettings settings, CharStream stream, RootDeclarativeRegion rootScope, LibraryDeclarativeRegion libraryScope)
			throws RecognitionException {
		final VhdlAntlrLexer lexer = new VhdlAntlrLexer(stream) {
			@Override
			public void emitErrorMessage(String msg) {
				// super.emitErrorMessage(msg);
			}
		};
		final CommonTokenStream ts = new CommonTokenStream(lexer);

		final VhdlAntlrParser parser = new VhdlAntlrParser(ts);
		final ParseErrorCollector parseErrorCollector = new ParseErrorCollector();
		parser.setErrorReporter(parseErrorCollector);
		parser.setTreeAdaptor(new TreeAdaptorWithoutErrorNodes());

		VhdlAntlrParser.design_file_return result;
		try {
			result = parser.design_file();
			if (!parseErrorCollector.errors.isEmpty()) {
				final VhdlFile dummy = new VhdlFile();
				Annotations.putAnnotation(dummy, ParseErrors.class, new ParseErrors(parseErrorCollector.errors));
				return dummy;
			}
			final CommonTreeNodeStream nodes = new CommonTreeNodeStream(result.getTree());
			nodes.setTokenStream(ts);
			final MetaClassCreator mcc = new MetaClassCreator(nodes, settings, rootScope, libraryScope);

			VhdlFile file = null;
			file = mcc.design_file();

			if (!mcc.getErrors().isEmpty()) {
				final List<ParseError> errors = mcc.getErrors();
				Annotations.putAnnotation(file, ParseErrors.class, new ParseErrors(errors));
				if (settings.isPrintErrors()) {
					reportErrors(errors);
				}
			}
			return file;
		} catch (final Exception e) {
			if (!parseErrorCollector.errors.isEmpty()) {
				final VhdlFile dummy = new VhdlFile();
				Annotations.putAnnotation(dummy, ParseErrors.class, new ParseErrors(parseErrorCollector.errors));
				return dummy;
			}
			throw e;
		}
	}

	public static VhdlFile parseStream(InputStream stream, VhdlParserSettings settings, RootDeclarativeRegion rootScope, LibraryDeclarativeRegion libray)
			throws IOException, RecognitionException {
		return parse(settings, new CaseInsensitiveInputStream(stream), rootScope, libray);
	}

	public static boolean hasParseErrors(VhdlFile file) {
		return Annotations.getAnnotation(file, ParseErrors.class) != null;
	}

	public static List<ParseError> getParseErrors(VhdlFile file) {
		final ParseErrors errors = Annotations.getAnnotation(file, ParseErrors.class);
		if (errors == null)
			return Collections.emptyList();
		return errors.getErrors();
	}

	private static String errorToMessage(ParseError error) {
		switch (error.getType()) {
		case UNKNOWN_COMPONENT:
			return "unknown component: " + error.getMessage();
		case UNKNOWN_CONFIGURATION:
			return "unknown configuration: " + error.getMessage();
		case UNKNOWN_CONSTANT:
			return "unknown constant: " + error.getMessage();
		case UNKNOWN_ENTITY:
			return "unknown entity: " + error.getMessage();
		case UNKNOWN_FILE:
			return "unknown file: " + error.getMessage();
		case UNKNOWN_LOOP:
			return "unknown loop: " + error.getMessage();
		case UNKNOWN_OTHER:
			return "unknown identifier: " + error.getMessage();
		case UNKNOWN_PACKAGE:
			return "unknown pacakge: " + error.getMessage();
		case UNKNOWN_SIGNAL:
			return "unknown signal: " + error.getMessage();
		case UNKNOWN_SIGNAL_ASSIGNMENT_TARGET:
			return "unknown signal assignment target: " + error.getMessage();
		case UNKNOWN_TYPE:
			return "unknown type: " + error.getMessage();
		case UNKNOWN_VARIABLE:
			return "unknown variable: " + error.getMessage();
		case UNKNOWN_VARIABLE_ASSIGNMENT_TARGET:
			return "unknown variable assignment target: " + error.getMessage();
		default:
			return "unknown error";
		}
	}

	private static void reportErrors(List<ParseError> errors) {
		for (final ParseError error : errors) {
			System.err.println("line " + error.getPosition().getBegin().getLine() + ": " + errorToMessage(error));
		}

	}

	private static final class ParseErrorCollector implements VhdlAntlrParser.IErrorReporter {
		public List<ParseError> errors = Lists.newArrayList();

		@Override
		public void reportError(String arg0, String msg, String[] arg2, RecognitionException arg3) {
			final SourcePosition begin = new SourcePosition(arg3.line, arg3.charPositionInLine, arg3.index);
			int length = 1;
			if ((arg3.token != null) && (arg3.token.getText() != null)) {
				length = arg3.token.getText().length();
			}
			final SourcePosition end = new SourcePosition(arg3.line, arg3.charPositionInLine + length, arg3.index + length);
			final PositionInformation position = new PositionInformation(begin, end);
			final ParseError error = new ParseError(position, null, msg);
			errors.add(error);
		}
	}

	private static class TreeAdaptorWithoutErrorNodes extends CommonTreeAdaptor {

		@Override
		public Object errorNode(TokenStream input, Token start, Token stop, RecognitionException e) {
			return null;
		}
	}
}
