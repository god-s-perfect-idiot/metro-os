package net.sourceforge.opencamera;

import android.renderscript.Allocation;
import android.renderscript.RenderScript;

/**
 * Stub for Open Camera's generated RenderScript histogram script.
 * Metro Camera does not enable histogram / zebra / focus-peaking overlays,
 * so this class is never constructed at runtime. Kept only so Preview compiles.
 */
@SuppressWarnings("unused")
public class ScriptC_histogram_compute {
    public ScriptC_histogram_compute(RenderScript rs) {
        throw new UnsupportedOperationException("Histogram overlays are disabled in Metro Camera");
    }
    public void bind_histogram(Allocation a) {}
    public void bind_histogram_r(Allocation a) {}
    public void bind_histogram_g(Allocation a) {}
    public void bind_histogram_b(Allocation a) {}
    public void invoke_init_histogram() {}
    public void invoke_init_histogram_rgb() {}
    public void forEach_histogram_compute_rgb(Allocation in) {}
    public void forEach_histogram_compute_by_luminance(Allocation in) {}
    public void forEach_histogram_compute_by_value(Allocation in) {}
    public void forEach_histogram_compute_by_intensity(Allocation in) {}
    public void forEach_histogram_compute_by_lightness(Allocation in) {}
    public void forEach_generate_zebra_stripes(Allocation in, Allocation out) {}
    public void forEach_generate_focus_peaking(Allocation in, Allocation out) {}
    public void forEach_generate_focus_peaking_filtered(Allocation in, Allocation out) {}
    public void set_bitmap(Allocation a) {}
    public void set_zebra_stripes_threshold(int v) {}
    public void set_zebra_stripes_width(int v) {}
    public void set_zebra_stripes_foreground_r(int v) {}
    public void set_zebra_stripes_foreground_g(int v) {}
    public void set_zebra_stripes_foreground_b(int v) {}
    public void set_zebra_stripes_foreground_a(int v) {}
    public void set_zebra_stripes_background_r(int v) {}
    public void set_zebra_stripes_background_g(int v) {}
    public void set_zebra_stripes_background_b(int v) {}
    public void set_zebra_stripes_background_a(int v) {}
}
