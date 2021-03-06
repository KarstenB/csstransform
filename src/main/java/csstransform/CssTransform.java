package csstransform;

import java.io.File;
import java.util.List;

import com.phloc.commons.charset.CCharset;
import com.phloc.commons.io.file.SimpleFileIO;
import com.phloc.commons.state.ESuccess;
import com.phloc.css.ECSSVersion;
import com.phloc.css.decl.CSSMediaRule;
import com.phloc.css.decl.CSSSelector;
import com.phloc.css.decl.CSSSelectorSimpleMember;
import com.phloc.css.decl.CSSStyleRule;
import com.phloc.css.decl.CascadingStyleSheet;
import com.phloc.css.decl.ICSSSelectorMember;
import com.phloc.css.decl.ICSSTopLevelRule;
import com.phloc.css.reader.CSSReader;
import com.phloc.css.writer.CSSWriter;
import com.phloc.css.writer.CSSWriterSettings;

public class CssTransform {
	public static void main(String[] args) {
		if (args.length == 0) {
			System.out.println("Simply specify all input css files. The _deep.css file will be created in the same directory");
		}
		for (final String pathname : args) {
			final File cssInputFile = new File(pathname);
			if (cssInputFile.exists()) {
				System.out.println("Processing:" + cssInputFile);
				final CascadingStyleSheet css = readCSS30(cssInputFile);
				writeCSS30(css, new File(pathname.replaceAll("\\.css", ".min.css")), true);
				modifyRules(css.getAllRules());
				writeCSS30(css, new File(pathname.replaceAll("\\.css", "_deep.css")), false);
				writeCSS30(css, new File(pathname.replaceAll("\\.css", "_deep.min.css")), true);
			} else {
				System.err.println("The file:" + cssInputFile + " could not be found");
			}
		}
	}

	public static void modifyRules(List<ICSSTopLevelRule> rules) {
		for (final ICSSTopLevelRule rule : rules) {
			if (rule instanceof CSSMediaRule) {
				modifyRules(((CSSMediaRule) rule).getAllRules());
			}
			if (rule instanceof CSSStyleRule) {
				final CSSStyleRule cssStyleRule = (CSSStyleRule) rule;
				modifyStyleRules(cssStyleRule);
			}
		}
	}

	public static void modifyStyleRules(final CSSStyleRule cssStyleRule) {
		final List<CSSSelector> selectors = cssStyleRule.getAllSelectors();
		for (final CSSSelector cssSelector : selectors) {
			if (cssSelector.getMemberCount() > 0) {
				final ICSSSelectorMember first = cssSelector.getMemberAtIndex(0);
				if (first instanceof CSSSelectorSimpleMember) {
					final CSSSelectorSimpleMember simpleMember = (CSSSelectorSimpleMember) first;
					if (simpleMember.isElementName()) {
						final String name = simpleMember.getValue().trim().toLowerCase();
						if (name.equals("body") || name.equals("html")) {
							continue;
						}
					}
				}
				cssSelector.addMember(0, new CSSSelectorSimpleMember(" /deep/ "));
				cssSelector.addMember(0, new CSSSelectorSimpleMember("body"));
			}
		}
	}

	/**
	 * Read a CSS 3.0 declaration from a file using UTF-8 encoding.
	 *
	 * @param aFile
	 *            The file to be read. May not be <code>null</code>.
	 * @return <code>null</code> if the file has syntax errors.
	 */
	public static CascadingStyleSheet readCSS30(final File aFile) {
		// UTF-8 is the fallback if neither a BOM nor @charset rule is present
		final CascadingStyleSheet aCSS = CSSReader.readFromFile(aFile, CCharset.CHARSET_UTF_8_OBJ, ECSSVersion.CSS30);
		if (aCSS == null) {
			// Most probably a syntax error
			System.err.println("Failed to read CSS - please see previous logging entries!");
			return null;
		}
		return aCSS;
	}

	/**
	 * Write a CSS 3.0 declaration to a file using UTF-8 encoding.
	 *
	 * @param aCSS
	 *            The CSS to be written to a file. May not be <code>null</code>.
	 * @param aFile
	 *            The file to be written. May not be <code>null</code>.
	 * @param minified
	 *            TODO
	 * @return {@link ESuccess#SUCCESS} if everything went okay, and
	 *         {@link ESuccess#FAILURE} if an error occurred
	 */
	public static ESuccess writeCSS30(final CascadingStyleSheet aCSS, final File aFile, boolean minified) {
		// 1.param: version to write
		// 2.param: false== non-optimized output
		final CSSWriterSettings aSettings = new CSSWriterSettings(ECSSVersion.CSS30, minified);
		try {
			final CSSWriter aWriter = new CSSWriter(aSettings);
			// Write the @charset rule: (optional)
			aWriter.setContentCharset(CCharset.CHARSET_UTF_8_OBJ.name());
			// Write a nice file header
			aWriter.setHeaderText("This file was generated by CssTransform: https://github.com/KarstenB/csstransform");
			// Convert the CSS to a String
			final String sCSSCode = aWriter.getCSSAsString(aCSS);
			// Finally write the String to a file
			return SimpleFileIO.writeFile(aFile, sCSSCode, CCharset.CHARSET_UTF_8_OBJ);
		} catch (final Exception ex) {
			System.err.println("Failed to write the CSS to a file");
			ex.printStackTrace();
			return ESuccess.FAILURE;
		}
	}
}
