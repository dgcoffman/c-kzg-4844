package ethereum.ckzg4844;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class CKZG4844JNI {

  private static final String LIBRARY_NAME = "ckzg4844jni";
  private static final String PLATFORM_NATIVE_LIBRARY_NAME = System.mapLibraryName(LIBRARY_NAME);

  /**
   * Loads the appropriate native library based on your platform and the selected {@link Preset}
   *
   * @param preset the preset
   */
  public static void loadNativeLibrary(Preset preset) {
    String libraryResourcePath =
        "lib/" + System.getProperty("os.arch") + "/" + preset.name().toLowerCase() + "/"
            + PLATFORM_NATIVE_LIBRARY_NAME;
    InputStream libraryResource = CKZG4844JNI.class.getResourceAsStream(libraryResourcePath);
    if (libraryResource == null) {
      try {
        System.loadLibrary(LIBRARY_NAME);
      } catch (UnsatisfiedLinkError __) {
        String exceptionMessage = String.format(
            "Couldn't load native library (%s). It wasn't available at %s or the library path.",
            LIBRARY_NAME, libraryResourcePath);
        throw new RuntimeException(exceptionMessage);
      }
    } else {
      try {
        Path tempDir = Files.createTempDirectory(LIBRARY_NAME + "@");
        tempDir.toFile().deleteOnExit();
        Path tempDll = tempDir.resolve(PLATFORM_NATIVE_LIBRARY_NAME);
        tempDll.toFile().deleteOnExit();
        Files.copy(libraryResource, tempDll, StandardCopyOption.REPLACE_EXISTING);
        libraryResource.close();
        System.load(tempDll.toString());
      } catch (IOException ex) {
        throw new UncheckedIOException(ex);
      }
    }
  }

  public enum Preset {
    MAINNET(4096), MINIMAL(4);

    public final int fieldElementsPerBlob;

    Preset(int fieldElementsPerBlob) {
      this.fieldElementsPerBlob = fieldElementsPerBlob;
    }
  }

  public static final BigInteger BLS_MODULUS = new BigInteger(
      "52435875175126190479447740508185965837690552500527637822603658699938581184513");
  public static final int BYTES_PER_COMMITMENT = 48;
  public static final int BYTES_PER_PROOF = 48;
  public static final int BYTES_PER_FIELD_ELEMENT = 32;

  private CKZG4844JNI() {
  }

  /**
   * Calculates the bytes per blob based on the output from {@link #getFieldElementsPerBlob()}
   *
   * @return the bytes per blob
   */
  public static int getBytesPerBlob() {
    return getFieldElementsPerBlob() * BYTES_PER_FIELD_ELEMENT;
  }

  /**
   * Retrieves the compile-time configured FIELD_ELEMENTS_PER_BLOB. The value will be based on the
   * selected {@link Preset} when loading the native library.
   *
   * @return the field elements per blob
   */
  public static native int getFieldElementsPerBlob();

  /**
   * Loads the trusted setup from a file. Once loaded, the same setup will be used for all the
   * crypto native calls. To load a new setup, free the current one by calling
   * {@link #freeTrustedSetup()} and then load the new one. If no trusted setup has been loaded, all
   * the crypto native calls will throw a {@link RuntimeException}.
   *
   * @param file a path to a trusted setup file
   * @throws CKZGException if there is a crypto error
   */
  public static native void loadTrustedSetup(String file);

  /**
   * An alternative to {@link #loadTrustedSetup(String)}. Loads the trusted setup from method
   * parameters instead of a file.
   *
   * @param g1      g1 values as bytes
   * @param g1Count the count of the g1 values
   * @param g2      g2 values as bytes
   * @param g2Count the count of the g2 values
   * @throws CKZGException if there is a crypto error
   */
  public static native void loadTrustedSetup(byte[] g1, long g1Count, byte[] g2,
      long g2Count);

  /**
   * Free the current trusted setup. This method will throw an exception if no trusted setup has
   * been loaded.
   */
  public static native void freeTrustedSetup();

  /**
   * Compute proof at point z for the polynomial represented by blob.
   *
   * @param blob blob bytes
   * @param z_bytes a point
   * @return the proof
   * @throws CKZGException if there is a crypto error
   */
  public static native byte[] computeKzgProof(byte[] blob, byte[] z_bytes);

  /**
   * Calculates aggregated proof for the given blobs
   *
   * @param blobs blobs as flattened bytes
   * @param count the count of the blobs
   * @return the aggregated proof
   * @throws CKZGException if there is a crypto error
   */
  public static native byte[] computeAggregateKzgProof(byte[] blobs, long count);

  /**
   * Verify aggregated proof and commitments for the given blobs
   *
   * @param blobs               blobs as flattened bytes
   * @param commitments_bytes   commitments as flattened bytes
   * @param count               the count of the blobs (should be same as the count of the commitments)
   * @param proof_bytes         the proof that needs verifying
   * @return true if the proof is valid and false otherwise
   * @throws CKZGException if there is a crypto error
   */
  public static native boolean verifyAggregateKzgProof(byte[] blobs, byte[] commitments_bytes, long count,
                                                       byte[] aggregated_proof_bytes);

  /**
   * Calculates commitment for a given blob
   *
   * @param blob blob bytes
   * @return the commitment
   * @throws CKZGException if there is a crypto error
   */
  public static native byte[] blobToKzgCommitment(byte[] blob);

  /**
   * Verify the proof by point evaluation for the given commitment
   *
   * @param commitment_bytes  commitment bytes
   * @param z_bytes           Z
   * @param y_bytes           Y
   * @param proof_bytes       the proof that needs verifying
   * @return true if the proof is valid and false otherwise
   * @throws CKZGException if there is a crypto error
   */
  public static native boolean verifyKzgProof(byte[] commitment_bytes, byte[] z_bytes, byte[] y_bytes,
                                              byte[] proof_bytes);

}
