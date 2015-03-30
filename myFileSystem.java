import java.util.*;
import java.io.*;
import java.nio.ByteBuffer;

public class myFileSystem{
	RandomAccessFile disk;

	byte[] freeBlockList = new byte[128];
	Inode[] inodeList = new Inode[16];
	byte[] emptyInode = new byte[56];

	public class Inode {
		char[] name;
		int size;
		int[] blockPointers=new int[8];
		private int start; //start byte location of inode in superblock
		int used; // 0 free, 1 not
		byte[] nameBytes; //use for comparing inodes

		public Inode(ByteBuffer name, int size, int loc) throws Exception{
			this.name=name.asCharBuffer().toString().toCharArray();
			this.size=size;
			start=loc;
			nameBytes=name.array();
			
			getPointers(); //sets the value in the blockPointers array and the use value
		}

		public void update() throws Exception{ //read from disk and set inode values with it
			disk.seek(start);

			ByteBuffer myName = ByteBuffer.allocate(16); //get name bytes
			disk.readFully(myName.array());
			name = myName.asCharBuffer().toString().toCharArray();
			nameBytes = myName.array();

			ByteBuffer mySize = ByteBuffer.allocate(4); //get size bytes
			disk.readFully(mySize.array());
			size = mySize.getInt();

			getPointers(); //sets the value in the blockPointers array and the use value
		}

		public void set(char[] fileName, int size) throws Exception{ //create sets inodes
			disk.seek(start);
			this.name = fileName;
			ByteBuffer fname = ByteBuffer.allocate(16); //translate to bytes
			for (int n=0; n<fileName.length; n++)
				fname.putChar(fileName[n]);

			disk.write(fname.array()); //update disk with name
			nameBytes=fname.array(); //name in bytes

			disk.write(ByteBuffer.allocate(4).putInt(size).array()); //update size in disk
			this.size=size;

			disk.seek(start+52); 
			disk.write(ByteBuffer.allocate(4).putInt(1).array()); //update use flag in disk
			this.used=1; //set used
		}

		public void getPointers() throws Exception{
			int index=0;
			for (int i=(start+20); i<(start+52); i+=4){
				ByteBuffer pointerVal = ByteBuffer.allocate(4);
				disk.seek(i);
				disk.readFully(pointerVal.array()); //get pointer bytes from disk
				
				blockPointers[index]=pointerVal.getInt(); //translate byte to int and set blockPointer
				index++;
			}

			ByteBuffer flag = ByteBuffer.allocate(4); 
			disk.readFully(flag.array()); //get used bytes
			used = flag.getInt(); //set used value
		}

		public void addPointer(int val) throws Exception{ //used in create
			for(int i=0; i<blockPointers.length; i++)
				if(blockPointers[i]==0){
					//System.out.println("Inode"+start/128 + " added block:" +val + " at block index "+i);
					blockPointers[i]=val; //update blockPointers array

					disk.seek(start+20+4*i); //start of blockPointers in disk
					disk.write(ByteBuffer.allocate(4).putInt(val).array()); //update disk 
					break;
				}
		}

		public String getName() throws Exception{ //translate name to string and return
			disk.seek(start);
			ByteBuffer fileName = ByteBuffer.allocate(16);
			disk.read(fileName.array());
			return fileName.asCharBuffer().toString();
		}
	}

	public void FileSystem(char[] diskName) throws Exception{
		String name="";
		for (int i=0; i<diskName.length; i++)
			name=name+diskName[i];

		disk = new RandomAccessFile(name, "rw");
		disk.seek(0);
		disk.readFully(freeBlockList); //populate freeblock list

		for (int j=0; j<emptyInode.length; j++)
			emptyInode[j]=0; //used in delete

		for (int i=0; i<16; i++){
			disk.seek(128+i*56);

			ByteBuffer inodeName = ByteBuffer.allocate(16); 
			disk.read(inodeName.array()); // get the inode names
			ByteBuffer inodeSize = ByteBuffer.allocate(4);
			disk.readFully(inodeSize.array()); //get size

			inodeList[i]= new Inode(inodeName, inodeSize.getInt(), 128+i*56); //add inodes to a list
		}
	}


	public void create(char[] name, int size) throws Exception {	
		System.out.println("C: Creating new file:"+String.valueOf(name)+" " +size);
		int fileSize=size;
		disk.seek(0);
		
		disk.readFully(freeBlockList); //reading in freeblocklist
		int numFreeBlocks=0; //get num free blocks
		for (int j=1; j<freeBlockList.length; j++)
			if(freeBlockList[j]==0)
				numFreeBlocks++;

		ByteBuffer fname = ByteBuffer.allocate(16); //get name in bytes
		for (int n=0; n<name.length; n++)
			fname.putChar(name[n]);

		//-------- does the file name already exist in the file?
		boolean exist=false; 
		for (int ii=0; ii<inodeList.length;ii++) //checks to see if filename already used
			if(Arrays.equals(inodeList[ii].nameBytes,fname.array())){
				System.out.println(" ERROR: A file with that name already exist!");
				exist=true;
				break;
			}

		
		if(numFreeBlocks>size && exist==false && size<=8){ //are there enough data blocks? Is size <=8?
			int freeInode=0; //find freeInode index in array
			
			for (int lookUp=0; lookUp<inodeList.length; lookUp++) //can for free inode in list
				if(inodeList[lookUp].used==0){
					freeInode=lookUp;
					inodeList[freeInode].set(name,size); //if free, update inode with info
					break;
				}

			String allocBlocks="";
			while(fileSize>0) //allocate all blocks to new file
				for (int f=1; f<freeBlockList.length; f++){
					if(fileSize<=0) break; //don't have any more blocks to allocate
					if (freeBlockList[f]==0){ //if block is free
						freeBlockList[f]=1; //then block is no longer free

						inodeList[freeInode].addPointer(f); //set pointers to blocks
						allocBlocks=allocBlocks+f+" ";

						fileSize--; //dec file size
					}
				}
			System.out.println(" Allocated Blocks: "+allocBlocks);
			disk.seek(0);
			disk.write(freeBlockList); // updates free blocks list
		}
	} 

	public void delete(char[] name) throws Exception{
		ByteBuffer fname = ByteBuffer.allocate(16); //get name in bytes
		for (int n=0; n<name.length; n++)
			fname.putChar(name[n]);

		for(int i=0; i<=inodeList.length; i++){ //finds file
			if(Arrays.equals(inodeList[i].nameBytes, fname.array())){ //if equal
				System.out.println("D: Deleting file "+ inodeList[i].getName());

				String allocBlocks="";
				for(int j=0; j<inodeList[i].blockPointers.length; j++) //looks at used blocks in freeBlockList
					if(inodeList[i].blockPointers[j]>0){
						allocBlocks=allocBlocks+inodeList[i].blockPointers[j]+" ";
						//System.out.println("Inode"+i+ " deleted block: "+(inodeList[i].blockPointers[j]));
						freeBlockList[(inodeList[i].blockPointers[j])]=0; //set blocks to not used
						
					}
				System.out.println(" Deallocated Blocks: "+allocBlocks);
				disk.seek(0);
				disk.write(freeBlockList); //update freeBlock list in disk
				disk.seek(inodeList[i].start);
				disk.write(emptyInode); //clear inode data from disk
				inodeList[i].update(); //clear inode data in program
				break;
			}
		}
	}

	public void ls() throws Exception{
		System.out.println("L: Listing Files"); //loops through inodeList array and prints
		for (int i=0; i<inodeList.length; i++)
			if(inodeList[i].used==1) //print inode if only it is used. 
				System.out.println(" File Name: "+inodeList[i].getName()+"; File Size: "+inodeList[i].size);
	}

	public void read(char[] name, int blocknum, char[] buf) throws Exception{
		ByteBuffer fname = ByteBuffer.allocate(16); //name in bytes
		for (int n=0; n<name.length; n++)
			fname.putChar(name[n]);

		for(int i=0; i<=inodeList.length; i++){
			if(Arrays.equals(inodeList[i].nameBytes, fname.array())){ //if equal we have located inode
				System.out.println("R: Reading Block "+blocknum+" of "+inodeList[i].getName());
				if(blocknum<inodeList[i].size){
					int blockIndex = inodeList[i].blockPointers[blocknum]; //find value of the blockPointer array at index of blocknum
					System.out.println(" --Disk Address of Block: "+blockIndex);
					
					ByteBuffer theBlock = ByteBuffer.allocate(1024); 
					disk.seek(blockIndex*1024); 
					disk.readFully(theBlock.array()); //theBlock now contains all the bytes in blockNum
					
					buf = theBlock.asCharBuffer().toString().toCharArray(); //sets buf
					System.out.println(" READ: "+theBlock.asCharBuffer().toString()); //print out read bytes
				}
				else
					System.out.println(" ERROR: "+inodeList[i].getName()+" does not have that many blocks");
				break;
				}
			}
	} 

	public void write(char[] name, int blockNum, char[] buf) throws Exception{
		ByteBuffer fname = ByteBuffer.allocate(16); //name in bytes
		for (int n=0; n<name.length; n++)
			fname.putChar(name[n]);

		for(int i=0; i<=inodeList.length; i++){
			if(Arrays.equals(inodeList[i].nameBytes, fname.array())){ //if equal we have located inode
				System.out.println("W: Writing Block" +blockNum +" of "+inodeList[i].getName());
				
				if(blockNum<inodeList[i].size){
					int blockIndex = inodeList[i].blockPointers[blockNum]; //disk addr
					System.out.println(" --Disk Address of Block: "+blockIndex);
					
					disk.seek(blockIndex*1024);
					ByteBuffer theBlock=ByteBuffer.allocate(1024); //use for conversion
					for (int f=0; f<buf.length; f++)
						theBlock.putChar(buf[f]);
					
					disk.write(theBlock.array()); //writes the char[] into file
					System.out.println(" WROTE: " +new String(theBlock.array(), "US-ASCII"));
				}
				else
					System.out.println(" ERROR: "+inodeList[i].getName()+" does not have that many blocks");
			break;
			}
		}

	}

	public enum Commands{ C, L, D, R, W } //used in switch() in main

	public static void main (String[] args) throws Exception{
		myFileSystem fs = new myFileSystem();
		
		FileInputStream input = new FileInputStream("testFile.txt"); 
		BufferedReader in = new BufferedReader(new InputStreamReader(input)); //used to read file
		
		String diskName = in.readLine(); //first line is diskName
		fs.FileSystem(diskName.toCharArray()); //set FileSystem
		
		char[] buf = new char[1024]; //used as 3rd param in R
		char[] hello = {'h','e','l','l','o'}; //for testing purposes of W

		String str;
		while((str = in.readLine())!=null){ //read in all other lines
			String[] createMe = str.split(" ");
			Commands enumVal = Commands.valueOf(createMe[0]); //make first letter from file a enum

			/*note: for testing purposes, parameters 3 of R and W are filled in */
			switch (enumVal) {
				case C: fs.create(createMe[1].toCharArray(), Integer.parseInt(createMe[2])); break;
				case L: fs.ls();break;
				case D: fs.delete(createMe[1].toCharArray()); break;
				case R: fs.read(createMe[1].toCharArray(), Integer.parseInt(createMe[2]), buf); break;
				case W: fs.write(createMe[1].toCharArray(), Integer.parseInt(createMe[2]), hello); break;
			}

		}

	}

}