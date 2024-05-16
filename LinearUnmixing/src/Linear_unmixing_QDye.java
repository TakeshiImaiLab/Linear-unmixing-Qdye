import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.plugin.*;
import ij.io.OpenDialog;
import Jama.Matrix;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;


public class Linear_unmixing_QDye implements PlugIn {

    private static final int MAX_CHANNELS = 8;

    public void run(String arg) {
        // Initialize image list
        List<ImagePlus> selectedImages = new ArrayList<>();

        // Image Selection
        int[] wList = WindowManager.getIDList();
        if (wList == null) {
            IJ.noImage();
            return;
        }
        String[] titles = new String[wList.length + 1];
        titles[0] = "-Not selected-";
        for (int i = 0; i < wList.length; i++) {
            ImagePlus imp = WindowManager.getImage(wList[i]);
            titles[i + 1] = imp != null ? imp.getTitle() : "";
        }

        // Create Dialog
        GenericDialog gd = new GenericDialog("Multi-Color Image Selection");
        gd.addChoice("Multi-Color Image:", titles, titles[0]);
        gd.addMessage("If the channels of the image are displayed in separate windows, do not select anything and click OK.");
        gd.showDialog();

        if (gd.wasCanceled()) return;

        int multiColorIndex = gd.getNextChoiceIndex();
        String baseName = ""; 
        boolean isBaseNameSet = false; // 

        if (multiColorIndex > 0) {
            ImagePlus multiColorImage = WindowManager.getImage(wList[multiColorIndex - 1]);
            baseName = multiColorImage.getTitle();
            isBaseNameSet = true;

            // Processing Multicolor Images
            if (multiColorImage != null && multiColorImage.getNChannels() > 1) {
                for (int c = 1; c <= multiColorImage.getNChannels(); c++) {
                    ImageStack channelStack = new ImageStack(multiColorImage.getWidth(), multiColorImage.getHeight());
                    for (int z = 1; z <= multiColorImage.getNSlices(); z++) {
                        ImageProcessor ip = multiColorImage.getStack().getProcessor(multiColorImage.getStackIndex(c, z, 1));
                        channelStack.addSlice(ip);
                    }
                    selectedImages.add(new ImagePlus("Channel " + c, channelStack));
                }
            }
        } else {
            // Select new image
            GenericDialog newGd = new GenericDialog("New Image Selection");
            for (int i = 0; i < MAX_CHANNELS; i++) {
                newGd.addChoice("Channel " + (i + 1) + ":", titles, titles[0]);
            }
            newGd.showDialog();
            if (newGd.wasCanceled()) return;

            for (int i = 0; i < MAX_CHANNELS; i++) {
                int index = newGd.getNextChoiceIndex();
                if (index > 0) {
                    ImagePlus img = WindowManager.getImage(wList[index - 1]);
                    if (!isBaseNameSet) {
                        baseName = img.getTitle();
                        isBaseNameSet = true;
                    }
                    selectedImages.add(img);
                }
            }
        }


    	    int selectedChannelCount = selectedImages.size();

    	    if (selectedChannelCount < 2) {
    	        IJ.error("At least two channels must be selected.");
    	        return;
    	    }
    
        double[][] matrixValues = getUserInputMatrix(selectedChannelCount);
        if (matrixValues == null) {
            return; 
        }
        //  computation of the inverse matrix

        Matrix matrix = new Matrix(matrixValues);
        Matrix inverseMatrix;
        try {
            inverseMatrix = matrix.inverse();
        } catch (RuntimeException e) {
            IJ.error("Could not calculate the inverse matrix.");
            return;
        }
        applyMatrixToImages(selectedImages, inverseMatrix, baseName);
    }

    private double[][] getUserInputMatrix(int channelCount) {
        JDialog dialog = new JDialog();
        dialog.setTitle("Matrix Input");
        dialog.setModal(true);
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        JTextField[][] fields = new JTextField[channelCount][channelCount];

        for (int i = 0; i < channelCount; i++) {
            JPanel rowPanel = new JPanel();
            rowPanel.add(new JLabel("Ch " + (i + 1) + ":"));
            for (int j = 0; j < channelCount; j++) {
                fields[i][j] = new JTextField(5);
                if (i == j) {
                    fields[i][j].setText("1.0"); // Defaults to 1.0 on the diagonal
                }
                rowPanel.add(fields[i][j]);
            }
            mainPanel.add(rowPanel);
        }

        // Panel for CSV Import and Submit buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JButton loadCsvButton = new JButton("Load CSV");
        loadCsvButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            int returnValue = fileChooser.showOpenDialog(null);
            if (returnValue == JFileChooser.APPROVE_OPTION) {
                try {
                    loadMatrixFromCsv(fileChooser.getSelectedFile().getAbsolutePath(), fields);
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(dialog, "Error reading file: " + ex.getMessage());
                }
            }
        });
        
        buttonPanel.add(loadCsvButton);

        JButton submitButton = new JButton("Submit");
        submitButton.addActionListener(e -> dialog.dispose());
        buttonPanel.add(submitButton);

        mainPanel.add(buttonPanel);

        // Add a caution message
        JLabel noteLabel = new JLabel("Note: Any empty cells will be considered as having a value of zero.");
        mainPanel.add(noteLabel);

        dialog.getContentPane().add(mainPanel);
        dialog.pack();
        dialog.setVisible(true);

        double[][] matrix = new double[channelCount][channelCount];
        for (int i = 0; i < channelCount; i++) {
            for (int j = 0; j < channelCount; j++) {
                try {
                    matrix[i][j] = Double.parseDouble(fields[i][j].getText());
                } catch (NumberFormatException ex) {
                    matrix[i][j] = 0.0; // 不正な入力は0として扱う
                }
            }
        }
        return matrix;
    }


    private void loadMatrixFromCsv(String filePath, JTextField[][] fields) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            int row = 0;
            while ((line = br.readLine()) != null && row < fields.length) {
                String[] values = line.split(",");
                for (int col = 0; col < values.length && col < fields[row].length; col++) {
                    fields[row][col].setText(values[col]);
                }
                row++;
            }
        }
    }


    private void applyMatrixToImages(List<ImagePlus> images, Matrix inverseMatrix, String originalName) {
        if (images.size() != inverseMatrix.getRowDimension()) {
            IJ.error("The number of images does not match the number of rows in the matrix.");
            return;
        }

        int width = images.get(0).getWidth();
        int height = images.get(0).getHeight();
        int depth = images.get(0).getStackSize();
        int bitDepth = images.get(0).getBitDepth();

        ImageStack compositeStack = new ImageStack(width, height);

        for (int z = 1; z <= depth; z++) {
            for (int i = 0; i < images.size(); i++) {
                ImageProcessor newChannelProcessor = new FloatProcessor(width, height);

                for (int j = 0; j < images.size(); j++) {
                    ImageProcessor channelProcessor = images.get(j).getStack().getProcessor(z);
                    float[] newChannelPixels = (float[]) newChannelProcessor.getPixels();
                    float[] channelPixels = convertToFloatArray(channelProcessor.getPixels());
                    double matrixValue = inverseMatrix.get(i, j);
                    for (int k = 0; k < channelPixels.length; k++) {
                        newChannelPixels[k] += matrixValue * channelPixels[k];
                    }
                }
                compositeStack.addSlice(convertToOriginalDepth(newChannelProcessor, bitDepth));
            }
        }
        String newName = "Unmixed_" + originalName;
        ImagePlus compositeImage = new ImagePlus(newName, compositeStack);
        compositeImage.setDimensions(images.size(), depth, 1);
        CompositeImage finalComposite = new CompositeImage(compositeImage, CompositeImage.GRAYSCALE);
        finalComposite.show();
    }

    

    private void printMatrix(double[][] matrix) {
        for (double[] row : matrix) {
            for (double val : row) {
                System.out.print(val + " ");
            }
            System.out.println();
        }
    }

    private float[] convertToFloatArray(Object pixels) {
        if (pixels instanceof byte[]) {
            byte[] bytePixels = (byte[]) pixels;
            float[] floatPixels = new float[bytePixels.length];
            for (int i = 0; i < bytePixels.length; i++) {
                floatPixels[i] = (float) (bytePixels[i] & 0xff);
            }
            return floatPixels;
        } else if (pixels instanceof short[]) {
            short[] shortPixels = (short[]) pixels;
            float[] floatPixels = new float[shortPixels.length];
            for (int i = 0; i < shortPixels.length; i++) {
                floatPixels[i] = (float) (shortPixels[i] & 0xffff);
            }
            return floatPixels;
        } else if (pixels instanceof float[]) {
            return (float[]) pixels;
        } else {
            throw new IllegalArgumentException("Unsupported image type");
        }
    }

    private ImageProcessor convertToOriginalDepth(ImageProcessor processor, int bitDepth) {
        switch (bitDepth) {
            case 8:
                return convertToByteWithoutScaling(processor);
            case 16:
                return convertToShortWithoutScaling(processor);
            case 32:
                return processor; 
            default:
                throw new IllegalArgumentException("Unsupported bit depth: " + bitDepth);
        }
    }

    private ImageProcessor convertToByteWithoutScaling(ImageProcessor processor) {
        float[] floatPixels = (float[]) processor.getPixels();
        byte[] bytePixels = new byte[floatPixels.length];

        for (int i = 0; i < floatPixels.length; i++) {
            if (floatPixels[i] < 0) {
                bytePixels[i] = 0;
            } else if (floatPixels[i] > 255) {
                bytePixels[i] = (byte) 255;
            } else {
                bytePixels[i] = (byte) floatPixels[i];
            }
        }

        return new ByteProcessor(processor.getWidth(), processor.getHeight(), bytePixels, processor.getColorModel());
    }

    private ImageProcessor convertToShortWithoutScaling(ImageProcessor processor) {
        float[] floatPixels = (float[]) processor.getPixels();
        short[] shortPixels = new short[floatPixels.length];

        for (int i = 0; i < floatPixels.length; i++) {
            if (floatPixels[i] < 0) {
                shortPixels[i] = 0;
            } else if (floatPixels[i] > 65535) {
                shortPixels[i] = (short) 65535;
            } else {
                shortPixels[i] = (short) floatPixels[i];
            }
        }

        return new ShortProcessor(processor.getWidth(), processor.getHeight(), shortPixels, processor.getColorModel());
    }

}
