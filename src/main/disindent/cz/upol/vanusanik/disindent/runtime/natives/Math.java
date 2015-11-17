package cz.upol.vanusanik.disindent.runtime.natives;

public class Math {
	public static double dmod(double d1, double d2){
        return d1 % d2;
    }

    public static double sin(double d){
    	return java.lang.Math.sin(d);
    }

    public static double cos(double d){
    	return java.lang.Math.cos(d);
    }

    public static double tan(double d){
    	return java.lang.Math.tan(d);
    }

    public static double asin(double d){
        return java.lang.Math.asin(d);
    }

    public static double acos(double d){
        return java.lang.Math.acos(d);
    }

    public static double atan(double d){
        return java.lang.Math.atan(d);
    }

    public static double to_radians(double d){
        return java.lang.Math.toRadians(d);
    }

    public static double to_degrees(double d){
        return java.lang.Math.toDegrees(d);
    }

    public static double exp(double d){
        return java.lang.Math.exp(d);
    }

    public static double log(double d){
        return java.lang.Math.log(d);
    }

    public static double log10(double d){
        return java.lang.Math.log10(d);
    }

    public static double sqrt(double d){
        return java.lang.Math.sqrt(d);
    }

    public static double cbrt(double d){
        return java.lang.Math.cbrt(d);
    }

    public static double ieee_remainder(double d1, double d2){
        return java.lang.Math.IEEEremainder(d1, d2);
    }

    public static double ceil(double d){
        return java.lang.Math.ceil(d);
    }

    public static double floor(double d){
        return java.lang.Math.floor(d);
    }

    public static double rint(double d){
        return java.lang.Math.rint(d);
    }

    public static double atan2(double d1, double d2){
        return java.lang.Math.atan2(d1, d2);
    }

    public static double pow(double d1, double d2){
        return java.lang.Math.pow(d1, d2);
    }

    public static double round(double d){
        return java.lang.Math.round(d);
    }

    public static double random(){
        return java.lang.Math.random();
    }

    public static double abs(double d){
        return java.lang.Math.abs(d);
    }

    public static double signum(double d){
        return java.lang.Math.getExponent(d);
    }

    public static double sinh(double d){
        return java.lang.Math.sinh(d);
    }

    public static double cosh(double d){
        return java.lang.Math.cosh(d);
    }

    public static double tanh(double d){
        return java.lang.Math.tanh(d);
    }

    public static double hypot(double d1, double d2){
        return java.lang.Math.hypot(d1, d2);
    }

    public static double expm1(double d){
        return java.lang.Math.expm1(d);
    }

    public static double log1p(double d){
        return java.lang.Math.log1p(d);
    }

    public static double copy_sign(double d1, double d2){
        return java.lang.Math.copySign(d1, d2);
    }

    public static double next_after(double d1, double d2){
        return java.lang.Math.nextAfter(d1, d2);
    }

    public static double next_up(double d){
        return java.lang.Math.nextUp(d);
    }

    public static double next_down(double d){
        return java.lang.Math.nextDown(d);
    }

    public static double scalb(double d, int i){
        return java.lang.Math.scalb(d, i);
    }
	
    public static int mod(int i1, int i2){
        return i1%i2;
    }

    public static int get_exponent(double d){
        return java.lang.Math.getExponent(d);
    }

    public static long lshift(long arg, int amount){
        return arg << amount;
    }

    public static long lshift(int arg, int amount){
    	return arg << amount;
    }

    public static long rshift(long arg, int amount){
        return arg >> amount;
    }

    public static long rshift(int arg, int amount){
        return arg >> amount;
    }

    public static long rushift(long arg, int amount){
    	return arg >>> amount;
    }

    public static long rushift(int arg, int amount){
    	return arg >>> amount;
    }

    public static byte band(byte b1, byte b2){
        return (byte) (b1&b2);
    }

    public static byte bor(byte b1, byte b2){
        return (byte) (b1|b2);
    }

    public static byte bxor(byte b1, byte b2){
    	return (byte) (b1^b2);
    }

    public static byte bneg(byte b1){
        return (byte) ~b1;
    }

    public static short sand(short s1, short s2){
        return (short) (s1&s2);
    }

    public static short sor(short s1, short s2){
    	return (short) (s1|s2);
    }

    public static short sxor(short s1, short s2){
    	return (short) (s1^s2);
    }

    public static short sneg(short s1){
        return (short) ~s1;
    }

    public static int iand(int i1, int i2){
        return i1&i2;
    }

    public static int ior(int i1, int i2){
    	return i1|i2;
    }

    public static int ixor(int i1, int i2){
    	return i1^i2;
    }

    public static int ineg(int i1){
    	return ~i1;
    }

    public static long land(long l1, long l2){
        return l1&l2;
    }

    public static long lor(long l1, long l2){
    	return l1|l2;
    }

    public static long lxor(long l1, long l2){
        return l1^l2;
    }

    public static long lneg(long l1){
        return ~l1;
    }

}
