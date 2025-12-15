package garden.hestia.hoofprint.util;

public class MathUtil {
	public static int roundToBase(int a, int base) {
		return a - a % base;
	}
}
