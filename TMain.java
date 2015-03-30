import java.io.*;

public class TMain {

  public void createFS (String FileName) {
    System.out.println("Creating a 128 KB file [" + FileName + "]");
    System.out.println("This file will act as a dummy disk and will hold your filesystem.");

    FileOutputStream F = null;
    try {
      F = new FileOutputStream(FileName);
    } catch (Exception E) {
      System.err.println ("Error creating the file.");
      System.err.println ("Message : " + E.getMessage());
      System.err.println ("Format unsuccessful - disk unusable");
      System.exit (1);
    }

    System.out.println("Formatting filesystem...");
    byte [] Block = new byte [1024];
    int i;

    for (i = 0; i < Block.length; i++) Block[i] = 0;

    /* write super block */
    Block [0] = 1; /* mark superblock as allocated in the free block list
                      all other blocks are free, all inodes are zeroed out */

    /* write out the super block */
    try {
      F.write (Block);
    } catch (Exception E) {
      System.err.println ("Error writin Block [0]");
      System.err.println ("Message : " + E.getMessage());
      System.err.println ("Format unsuccessful - disk unusable");
      System.exit (1);
    }

    /* write out the remaining 127 data blocks, all zeroed out */

    Block[0]=0;
    for(i = 0; i < 127; i++)
      try {
        F.write (Block);
      } catch (Exception E) {
        System.err.println ("Error writin Block [" + i + "]");
        System.err.println ("Message : " + E.getMessage());
        System.err.println ("Format unsuccessful - disk unusable");
        System.exit (1);
      }

    try {
      F.close();
      } catch (Exception E) {
        System.err.println ("Error finalizing format.");
        System.err.println ("Message : " + E.getMessage());
        System.err.println ("Format unsuccessful - disk unusable");
        System.exit (1);
    }

    System.out.println ("Format successful.");
  }

  public static void main (String [] args) {

    System.out.println();
    System.out.println();

    if (args.length == 0) {
      System.err.println ("Please pass the dummy disk filename as a parameter.");
      System.exit(0);
    }

    new TMain().createFS (args[0]);
  }
}