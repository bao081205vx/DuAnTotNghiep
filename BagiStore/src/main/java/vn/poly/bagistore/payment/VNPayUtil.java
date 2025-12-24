package vn.poly.bagistore.payment;

/**
 * Legacy placeholder for VNPay utilities.
 *
 * The old static VNPayUtil implementation has been replaced by a
 * Spring-managed `VNPayService` and configuration class `VNPayConfig`.
 *
 * Keep this class as a stub to avoid compilation issues if some code
 * still imports it; calling methods will throw an exception.
 */
public final class VNPayUtil {
    private VNPayUtil() { }

    public static String deprecatedBuildPaymentUrl() {
        throw new UnsupportedOperationException("VNPayUtil is deprecated; use VNPayService bean instead");
    }
}
