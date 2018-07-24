package gov.nasa.gsfc.spdf.cdfj;
public class Stride {
    int[] stride;
    int nv;
    public Stride(int[] stride) {
        if (stride.length == 0) {
            this.stride = null;
            return;
        }
        if (stride.length == 1) {
            this.stride = new int[]{stride[0]};
        } else {
            this.stride = new int[]{stride[0], stride[1]};
        }
    }
    public int getStride(int nv) {
        this.nv = nv;
        return getStride();
    }
    public int getStride() {
        int _stride = 1;
        if (stride != null) {
            if (stride[0] > 0) {
                _stride = stride[0];
            } else {
                if (nv > stride[1]) {
                    _stride = (nv/stride[1]);
                    if (_stride*stride[1] < nv) _stride++;
                }
            }
        }
        return _stride;
    }
}
