package org.eclipse.tm4e.core.internal.utils;

import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.Nullable;

/**
 * @see <a href=
 *      "https://github.com/microsoft/vscode-textmate/blob/e8d1fc5d04b2fc91384c7a895f6c9ff296a38ac8/src/utils.ts">
 *      https://github.com/microsoft/vscode-textmate/blob/main/src/utils.ts</a>
 */
public final class StringUtils {

	private static final Pattern RRGGBB = Pattern.compile("^#[0-9a-f]{6}", Pattern.CASE_INSENSITIVE);
	private static final Pattern RRGGBBAA = Pattern.compile("^#[0-9a-f]{8}", Pattern.CASE_INSENSITIVE);
	private static final Pattern RGB = Pattern.compile("^#[0-9a-f]{3}", Pattern.CASE_INSENSITIVE);
	private static final Pattern RGBA = Pattern.compile("^#[0-9a-f]{4}", Pattern.CASE_INSENSITIVE);

	public static boolean isValidHexColor(final CharSequence hex) {
		if (hex.length() == 0) {
			return false;
		}

		if (RRGGBB.matcher(hex).matches()) {
			// #rrggbb
			return true;
		}

		if (RRGGBBAA.matcher(hex).matches()) {
			// #rrggbbaa
			return true;
		}

		if (RGB.matcher(hex).matches()) {
			// #rgb
			return true;
		}

		if (RGBA.matcher(hex).matches()) {
			// #rgba
			return true;
		}

		return false;
	}

	public static int strcmp(final String a, final String b) {
		final int result = a.compareTo(b);
		if (result < 0) {
			return -1;
		} else if (result > 0) {
			return 1;
		}
		return 0;
	}

	public static int strArrCmp(@Nullable final List<String> a, @Nullable final List<String> b) {
		if (a == null && b == null) {
			return 0;
		}
		if (a == null) {
			return -1;
		}
		if (b == null) {
			return 1;
		}
		final int len1 = a.size();
		final int len2 = b.size();
		if (len1 == len2) {
			for (int i = 0; i < len1; i++) {
				final int res = strcmp(a.get(i), b.get(i));
				if (res != 0) {
					return res;
				}
			}
			return 0;
		}
		return len1 - len2;
	}

	/**
	 * @return "{SimpleClassName}{...fields...}"
	 */
	public static String toString(@Nullable final Object object, final Consumer<StringBuilder> fieldsBuilder) {
		if (object == null)
			return "null";
		final var sb = new StringBuilder(object.getClass().getSimpleName());
		sb.append('{');
		fieldsBuilder.accept(sb);
		sb.append('}');
		return sb.toString();
	}

	private StringUtils() {
	}
}
