package gecko.geckocircuits.circuit;

abstract class AbstractCachedMatrix {

    protected int _hashCode = -1;
    protected long _secondHashCode = -1;

    // use prime numbers, here:
    protected static final int HASH_7 = 7;
    protected static final int HASH_13 = 13;
    protected static final int HASH_17 = 17;
    protected static final int HASH_23 = 23;
    protected static final int HASH_37 = 37;

    private static final int INT_LENGTH = 32;
    private double _latestAccessTime = -1;
    private int _accessCounter = 0;
    protected double[][] _originalMatrix;

    public AbstractCachedMatrix(final double[][] matrix) {
        _originalMatrix = matrix;
    }

    abstract void initLUDecomp();

    abstract public void deleteCache();

    abstract public double[] solve(final double[] bVector);

    abstract int calculateMemoryRequirement();

    public long secondHashCode() {
        if (_secondHashCode == -1) {
            long newHashCode = 0;

            for (int i = 0; i < _originalMatrix.length; i++) {
                newHashCode += java.util.Arrays.hashCode(_originalMatrix[i]) * (991 * (i + 3));
            }
            _secondHashCode = newHashCode;
        }

        return _secondHashCode;
    }

    @Override
    public final int hashCode() {
        if (_hashCode == -1) {
            long newHashCode = HASH_13;
            for (int i = 0; i < _originalMatrix.length; i++) {
                newHashCode += java.util.Arrays.hashCode(_originalMatrix[i]) * (829 * (i + 7));                
            }
            _hashCode = (int) ((int) (newHashCode ^ (newHashCode >>> INT_LENGTH)));
        }
        return _hashCode;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final AbstractCachedMatrix other = (AbstractCachedMatrix) obj;

        if (other._originalMatrix.length != this._originalMatrix.length) {
            return false;
        }

        for (int i = 0; i < _originalMatrix.length; i++) {
            for (int j = 0; j < _originalMatrix[0].length; j++) {
                if (_originalMatrix[i][j] != other._originalMatrix[i][j]) {
                    return false;
                }
            }
        }

        return true;
    }

    protected void setAccess(final double time) {
        _accessCounter++;
        _latestAccessTime = time;
    }

    protected int getAccessCounter() {
        return _accessCounter;
    }

    protected double getLatestAccessTime() {
        return _latestAccessTime;
    }
}
