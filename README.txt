Xian Chen 
377 Lab3: File Systems
Documentation


Introduction: 
The purpose of this document is to outline my design choices when implementing a persistent file system. This discussion of the program code will be divided into the following parts: Brief Overview, FileSystem(), Inode class, create(), delete(), ls(), read() and write(). Design choices, and algorithms used in the program will be discussed in these sections. Choices in data structures and data types will be discussed after. At the end of the code discussion are a list of references 

Brief Overview:
The goal of this lab assignment is create a persistent file system. The file system is segmented into 128 blocks which are 1KB in size. The first block contains the superblock which keeps track of the free block list and the inodes. In the program, an array is created for the free block list and the inodes. The array for the free blocks is simply an integer array that gets set to 0 if the block is not used, and 1 if the block is used. The inode list is a arraylist of Inode objects. To do so, a new Inode class is created that keep stores of the inode name, size, location (start address of the inode block in the superblock), blockPointers, and used flag (0 if free, 1 otherwise). The first thing the program does when it starts is to read in the superblock from the disk and to initialize the free block list and the inode list. Therefore, all 16 inodes are initialized in the beginning. When a file is created, these inodes are updated and in turn, the inodes updates the disk. The free block list array in the program will also be updated and written to the disk. In the same manner files can be deleted, read, written, and listed using the inode list and the free block list (and writing to the disk after each change). 

FileSystem():
This method is called at the beginning of the program. The program reads in the input file and pass to this method the name of the disk file to be read from. The method then reads the first 128 bytes from the disk and populates the freeBlockList (which is created as a global variable. It will also initialized a byte[56] (global variable) of 0s which will be later used in delete). The method then populates the inodeList by creating 16 inodes. This is done by observing in the bytes corresponding to to the inode’s name, size, location (start address for each inode in superblock), converting them into char[] and ints respectively, and passing them in as parameters in the inode constructure.  

Inode Class:
The inode class stores a name, size, bytePointers, used flag, and address. It also have an additional name field in bytes called nameBytes. This offers a more accurate way to compare names which is needed in the read(), write() and delete methods. The main function of the Inode class is to read from and write to the disk file. It contains the following methods: 

- update(): 
update reads the inode’s block of data from the superblock (56 bytes in length) and uses it to update the inode’s information (name, size.. etc.). Is called by delete() when this inode data block (in the superblock) has been written to the ask as all 0s (deleted). 

- set(): 
set is called when a file is created. The inode set its information to those that were passed to it (from the parameters) and writes them to disk.

- getPointers(): 
this methods reads from the disk the current node’s blockPointer bytes and updates the inodes’ blockPointer array with the information read. 

- addPointer(): 
called by create() when it allocates a block to the inode. addPointer adds the address of the allocated block into the inode’s blockPointer array (which is passed in the parameters) and writes the updated information into file.

- getName(): 
converts the name bytes from the disk into a String to be used for easy printing. 

Create():
This method attempts to create a file of a certain size. It first reads in the freeBlockList and count the number of free blocks available. If the number of blocks required for the file is not greater than the number of available blocks, then it cannot be created. Otherwise, a free Inode (which can be found by looping through the Inode array and observing each inode’s used value), is updated to store that file name and file size. The method then allocates blocks to the designated inode by looping through the freeBlockList to find free blocks and then setting them to used (1). It then calls addPointer(x) on the inode. This is done “size” amount of times-- therefore the method allocated size number of blocks to the inode and links them with the inode’s blockPointer. 


Delete():
Delete loops through the inode list and compare the names of each inode in bytes to the char[] array that was passed in also converted to bytes. If there is a match, then that inode’s blocks are freed. This is done by looking at the blockPointer values in the inode’s array. The blockPointer values are the addresses of the blocks in the freeBlocksList. We reference the linked block in the freeBlockList and set its flag in to “0”. The updated list is then written to disk. An empty inode (which was created in FileSystem) is then written to the disk at the start address of the to-be-deleted inode. This inode then calls update() to read its changed bytes from the disk and update (delete) its stored information.

Ls():
This method loops through the Inode list and prints out the name and size of all the Inodes that have its used flag set to 1. 

Read():
Read loops through the Inode list to find a name that matches the filename that was passed in (compare in bytes). When a match is found, it looks at the blockPointer of that inode and look at its value at the blockNum value that was passed in. It then reference the block’s address from disk and reads in 1024 bytes. The read bytes are then converted into a char[] and the passed char[] buf is set to equal it. 

Write():
Like read(), write looks through the Inode list to find a filename that matches the one that was passed in (in bytes). It will then also look at that inode’s blockPointer at the location of the blockNum value that was passed in. This will give the starting address of a block. The method then converts the passed in char[] buf into bytes and write it into the disk starting at the address of the block obtained. 


Data Structures: Arrays (byte[], char[], int[]) are mostly used in this program rather than list (ArrayList, LinkedList.. etc) since they can be more easily converted between each other by using a ByteBuffer. Byte[] arrays are preferred since they do not need to be converted when reading or writing onto the disk. 

Data Types:
Enum: Used in the switch() statements in main rather than have multiple if statements. This names the code easier to read and shorter. 
RandomAccessFile: Simplifies seeking, reading and writing to and from files


Features not Implemented:
- The cast write/read into/from buffer out of bounds
- boundary tests


References:
Enum Types: http://docs.oracle.com/javase/tutorial/java/javaOO/enum.html
ByteBuffer: http://docs.oracle.com/javase/7/docs/api/java/nio/ByteBuffer.html
Converting byte[] to String: http://stackoverflow.com/questions/88838/how-to-convert-strings-to-and-from-utf8-byte-arrays-in-java
Converting int to byte[]: http://stackoverflow.com/questions/6374915/java-convert-int-to-byte-array-of-4-bytes
RandomAccessFiles: http://tutorials.jenkov.com/java-io/randomaccessfile.html

