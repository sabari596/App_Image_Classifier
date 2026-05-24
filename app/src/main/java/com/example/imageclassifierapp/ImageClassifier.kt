import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class ImageClassifier(private val context: Context) {

    private val inputSize = 224
    private val interpreter: Interpreter
    private val labels: List<String>

    init {
        interpreter = Interpreter(loadModel())
        labels = context.assets.open("labels.txt")
            .bufferedReader()
            .readLines()
            .map { it.substringAfter(" ") }
    }

    private fun loadModel(): MappedByteBuffer {
        val fd = context.assets.openFd("model_unquant.tflite")
        val inputStream = FileInputStream(fd.fileDescriptor)
        val channel = inputStream.channel
        return channel.map(
            FileChannel.MapMode.READ_ONLY,
            fd.startOffset,
            fd.declaredLength
        )
    }

    fun classify(bitmap: Bitmap): Pair<String, Float> {

        val resized = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)

        val buffer =
            ByteBuffer.allocateDirect(4 * inputSize * inputSize * 3)
        buffer.order(ByteOrder.nativeOrder())

        val pixels = IntArray(inputSize * inputSize)
        resized.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)

        for (pixel in pixels) {
            buffer.putFloat(((pixel shr 16) and 0xFF) / 255f)
            buffer.putFloat(((pixel shr 8) and 0xFF) / 255f)
            buffer.putFloat((pixel and 0xFF) / 255f)
        }

        val output = Array(1) { FloatArray(labels.size) }
        interpreter.run(buffer, output)

        val index = output[0].indices.maxByOrNull { output[0][it] } ?: -1
        return labels[index] to output[0][index] * 100
    }
}
