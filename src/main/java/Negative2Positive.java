import org.apache.commons.imaging.ImageFormats;
import org.apache.commons.imaging.Imaging;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * 类<code>Doc</code>用于：彩色胶片负片转正片
 *
 * @author jiaqi.zhang
 * @version 1.0
 * @date 2024-09-04
 */
public class Negative2Positive {
    // 定义文件夹路径常量
    private static final String INPUT_DIR = "src/main/java/testfile/input/";
    private static final String OUTPUT_DIR = "src/main/java/testfile/output/";

    public void processImages() {
        File inputDir = new File(INPUT_DIR);
        File[] imageFiles = inputDir.listFiles((dir, name) -> {
            String lowerName = name.toLowerCase();
            return lowerName.endsWith(".tif") || lowerName.endsWith(".tiff") ||
                    lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") ||
                    lowerName.endsWith(".png") || lowerName.endsWith(".bmp");
        });

        if (imageFiles == null || imageFiles.length == 0) {
            System.out.println("No image files found in the specified directory.");
            return;
        }

        // 使用并行流处理图像文件
        List<File> fileList = Arrays.asList(imageFiles);
        fileList.parallelStream().forEach(imageFile -> {
            try {
                processImage(imageFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void processImage(File imageFile) throws IOException {
        BufferedImage imageBuilder = Imaging.getBufferedImage(imageFile);

        // 反相 RGB 曲线
        invertImage(imageBuilder);

        // 保存反相后的图像
        //String invertedImagePath = OUTPUT_DIR + "inverted_" + imageFile.getName();
        //File outputImageFile = new File(invertedImagePath);
        //Imaging.writeImage(imageBuilder, outputImageFile, ImageFormats.TIFF);
        //System.out.println("Image inversion complete. Inverted image saved: " + invertedImagePath);

        // 调用 changeP 方法
        BufferedImage changedImage = changeP(imageBuilder);

        // 保存调整后的图像
        String changedImagePath = OUTPUT_DIR + "changed_" + imageFile.getName();
        File outputChangedImageFile = new File(changedImagePath);
        Imaging.writeImage(changedImage, outputChangedImageFile, ImageFormats.TIFF);
        System.out.println("Image changeP complete. Changed image saved: " + changedImagePath);
    }

    /**
     * 反相图像的 RGB 值
     *
     * @param imageBuilder 要反相的图像
     */
    private void invertImage(BufferedImage imageBuilder) {
        int width = imageBuilder.getWidth();
        int height = imageBuilder.getHeight();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = imageBuilder.getRGB(x, y);
                int invertedRGB = invertRGB(rgb);
                imageBuilder.setRGB(x, y, invertedRGB);
            }
        }
    }

    /**
     * 反相单个像素的 RGB 值
     *
     * @param rgb 原始 RGB 值
     * @return 反相后的 RGB 值
     */
    private int invertRGB(int rgb) {
        int alpha = (rgb >> 24) & 0xFF;
        int red = 255 - ((rgb >> 16) & 0xFF);
        int green = 255 - ((rgb >> 8) & 0xFF);
        int blue = 255 - (rgb & 0xFF);
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    /**
     * 调整图像的 RGB 值，使其在 0 到 255 之间
     *
     * @param imageBuilder 要调整的图像
     * @return 调整后的图像
     */
    public BufferedImage changeP(BufferedImage imageBuilder) {
        int width = imageBuilder.getWidth();
        int height = imageBuilder.getHeight();

        // 初始化最大值和最小值
        int[] minMaxRed = findMinMax(imageBuilder, width, height, 16);
        int[] minMaxGreen = findMinMax(imageBuilder, width, height, 8);
        int[] minMaxBlue = findMinMax(imageBuilder, width, height, 0);

        // 遍历图片，调整像素 RGB 值
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = imageBuilder.getRGB(x, y);
                int adjustedRGB = adjustRGB(rgb, minMaxRed, minMaxGreen, minMaxBlue);
                imageBuilder.setRGB(x, y, adjustedRGB);
            }
        }
        return imageBuilder;
    }

    /**
     * 找到图像中某个通道的最小值和最大值
     *
     * @param image  图像
     * @param width  图像宽度
     * @param height 图像高度
     * @param shift  通道的位移量（红色为16，绿色为8，蓝色为0）
     * @return 包含最小值和最大值的数组
     */
    private int[] findMinMax(BufferedImage image, int width, int height, int shift) {
        int min = 255;
        int max = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int value = (image.getRGB(x, y) >> shift) & 0xFF;
                min = Math.min(min, value);
                max = Math.max(max, value);
            }
        }
        return new int[]{min, max};
    }

    /**
     * 调整单个像素的 RGB 值
     *
     * @param rgb         原始 RGB 值
     * @param minMaxRed   红色通道的最小值和最大值
     * @param minMaxGreen 绿色通道的最小值和最大值
     * @param minMaxBlue  蓝色通道的最小值和最大值
     * @return 调整后的 RGB 值
     */
    private int adjustRGB(int rgb, int[] minMaxRed, int[] minMaxGreen, int[] minMaxBlue) {
        int alpha = (rgb >> 24) & 0xFF;
        int red = adjustChannel((rgb >> 16) & 0xFF, minMaxRed);
        int green = adjustChannel((rgb >> 8) & 0xFF, minMaxGreen);
        int blue = adjustChannel(rgb & 0xFF, minMaxBlue);
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    /**
     * 调整单个通道的值
     *
     * @param value  原始值
     * @param minMax 最小值和最大值的数组
     * @return 调整后的值
     */
    private int adjustChannel(int value, int[] minMax) {
        int min = minMax[0];
        int max = minMax[1];
        return (value <= min) ? 0 : (value >= max) ? 255 : (value - min) * 255 / (max - min);
    }
}
