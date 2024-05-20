# Linear-unmixing-Qdye

# Overview
  When imaging samples with two or more fluorescent dyes, we need to obtain images at different excitation/emission ranges. 
  However, it is still difficult to obtain fluorescence signals for just one type of dye when samples are labeled with multiple types of dyes with overlapping excitation/emission spectra. 
  The ImageJ plugin "Linear unmixing QDye" can separate mixed signals based on the reference data for each of the dyes.　

  In the linear unmixing procedure, we assume that the total fluorescence signal at each pixel in an image is a linear sum of the signals derived from each of the fluorescent dyes. If we measure the crosstalk of each dye between channels beforehand, we can separate the signals derived from a dye using the inverse of the crosstalk matrix.

# What we need 
To separate fluorescence signals for n types of dyes (Dye 1, …, and Dye n), we need to take images under n different spectral conditions (channels) that can best separate n types of dyes. 
  Then we need to obtain reference signals for each of the dyes, by which we can know how much crosstalk will occur between the channels.  
![image](https://github.com/daichimori/Linear-unmixing-Qdye/assets/46915220/a0211868-cf46-429d-b811-76c52f619ee7)


# Installation
  If you don't have Fiji, you can download it from here ( https://imagej.net/Fiji#Downloads).
  To install the plugin, place the Linear_unmixing_QDye.jar file in the Fiji/plugins folder of ImageJ/Fiji. 

# Workflow 
  Open the multichannel image and launch the plugin “Linear_unmixing_QDye”. When the following pop-up window appears, select the target multichannel image and click "OK".
![image](https://github.com/daichimori/Linear-unmixing-Qdye/assets/46915220/22f2f070-5327-4dfc-801a-1fc468857e62)

  If each channel image opens in a separate window, press “OK” without selecting anything. A pop-up window will then appear. Select an image for each channel.

![image](https://github.com/daichimori/Linear-unmixing-Qdye/assets/46915220/a1d98781-05ea-40fe-93fa-7ff999f53fe1)



![image](https://github.com/daichimori/Linear-unmixing-Qdye/assets/46915220/71cbbdff-3e9d-4f6d-a6ef-019e75b0316c)

  The image above shows a screen to enter the crosstalk matrix, so that you can fill in the parameters for linear unmixing based on the reference data. 
    Enter the values for each of the fluorescent dyes on the vertical axis and the values for each channel on the horizontal axis.

  If you want to use the same parameters repeatedly, you can prepare a csv file with the crosstalk parameters. Click the “Load CSV" button and select the csv file; the values will be loaded automatically.

![image](https://github.com/daichimori/Linear-unmixing-Qdye/assets/46915220/bf2ec7e4-18a5-4dc2-abaa-db9344776249)
![image](https://github.com/daichimori/Linear-unmixing-Qdye/assets/46915220/bb1eca03-3f14-4ff3-adab-b426a662ed78)

  If no value is entered in a cell, it is treated as 0.


  The resulting image will be displayed in a new window with the prefix "Unmixed_". 

![image](https://github.com/daichimori/Linear-unmixing-Qdye/assets/46915220/77ba6923-15df-421a-ba29-fcb7dfd520f0)

# Reference 
Leiwe MN, Fujimoto S, Baba T, Moriyasu D, Saha B, Sakaguchi R, Inagaki S, Imai T.
Automated neuronal reconstruction with super-multicolour fluorescence imaging
bioRxiv 2022.10.20.512984 (Nature Communications, in press)
https://www.biorxiv.org/content/10.1101/2022.10.20.512984v1

Zimmermann, T., Rietdorf, J. & Pepperkok, R. Spectral imaging and its applications in live cell microscopy. FEBS Lett 546, 87-92 (2003). 

Dickinson, M.E., Bearman, G., Tille, S., Lansford, R. & Fraser, S.E. Multi-spectral imaging and linear unmixing add a whole new dimension to laser scanning fluorescence microscopy. Biotechniques 31, 1272-+ (2001).
