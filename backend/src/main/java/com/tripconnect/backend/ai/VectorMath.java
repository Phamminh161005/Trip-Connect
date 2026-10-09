package com.tripconnect.backend.ai;

/** Phép tính trên véc-tơ embedding. Véc-tơ lưu ở dạng đã chuẩn hóa nên độ giống cosine = tích vô hướng. */
public final class VectorMath {

    private VectorMath() {
    }

    /** Chia cho độ dài để véc-tơ có độ dài 1 (Gemini chỉ tự chuẩn hóa khi lấy đủ số chiều mặc định). */
    public static float[] normalize(float[] v) {
        double sum = 0;
        for (float x : v) sum += (double) x * x;
        double norm = Math.sqrt(sum);
        if (norm == 0) return v.clone();
        float[] out = new float[v.length];
        for (int i = 0; i < v.length; i++) out[i] = (float) (v[i] / norm);
        return out;
    }

    /** Tích vô hướng; với hai véc-tơ đã chuẩn hóa thì là độ giống cosine, trong khoảng [-1, 1]. */
    public static double dot(float[] a, float[] b) {
        if (a.length != b.length) throw new IllegalArgumentException("Hai véc-tơ khác số chiều");
        double sum = 0;
        for (int i = 0; i < a.length; i++) sum += (double) a[i] * b[i];
        return sum;
    }

    /** Trung bình có trọng số rồi chuẩn hóa — "véc-tơ sở thích" từ các tour người dùng đã xem / đặt. */
    public static float[] weightedMean(java.util.List<float[]> vectors, java.util.List<Double> weights) {
        if (vectors.isEmpty()) throw new IllegalArgumentException("Không có véc-tơ");
        float[] out = new float[vectors.get(0).length];
        for (int k = 0; k < vectors.size(); k++) {
            float[] v = vectors.get(k);
            double w = weights.get(k);
            for (int i = 0; i < out.length; i++) out[i] += (float) (v[i] * w);
        }
        return normalize(out);
    }
}
