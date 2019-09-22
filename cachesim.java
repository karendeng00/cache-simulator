import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.io.FileNotFoundException;
import java.lang.Math;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ArrayList;

public class cachesim {
	public static void main(String[] args) throws FileNotFoundException {
	
		
		String filename = args[0];
		File f = new File(filename);
		int size = parseInt(args[1]);  
		int way = parseInt(args[2]);
		String write = args[3];
		int block = parseInt(args[4]);
		
		//calculate frames and sets
		int frame = (size * 1024) / block;
		int set = frame / way;
		
		//read from file
		
		Scanner sc = new Scanner(f);
		
		//create cache
		int[][] valid = new	int[set][way];
		int[][] tag_arr = new int[set][way];
		
		ArrayList<LRULinkedHashMap<Integer, String>> a = new ArrayList<LRULinkedHashMap<Integer, String>>(); 
		for(int i = 0; i < set; i++) {
			a.add(new LRULinkedHashMap<Integer, String>(way));
		}
		
		//index = set
		//way; data
		
		String[][] data = new String[set][way];
		for(int i = 0; i < set; i++) {
			for(int j = 0; j < way; j++) {
				for(int k = 0; k < block; k++) {
					data[i][j] += "00";
				}
			}
		}
		
		String[] mem = new String[(int)Math.pow(2, 16)];
		for(int i = 0; i < mem.length; i++) {
				mem[i] = "00";
		}
		
		int[][] addr = new int[set][way];
		int[][] dirty = new int[set][way];
		
		while(sc.hasNext()) {
			boolean c = false;
			String miss_hit = "miss";
		 	String file = sc.nextLine();
			String[] arr = file.split(" ");
			
			//store or load
			String name = arr[0];
			if(name.equals("store")) {
				c = true;
			}
			
			//each array index stores 2 bytes
			
			String hex = arr[1];
			String hex1 = dectobin(hextodec(hex));
			int address = (hextodec(hex));
			
			int block_a = (int)log2(block);
			int set_a = (int)log2(set);
			int tag_a = 16 - block_a - set_a;
			//System.out.println("block: " + block_a + " set: " + set_a + " tag_a: " + tag_a);
			
			//tag
			int tag = address / (set * block);
			
			//tells you which set
			int index = bintodec(hex1.substring(tag_a, tag_a + set_a));
			
			//tells you which byte
			int block_offset = bintodec(hex1.substring(tag_a + set_a, 16));
			
			//size of access - read from memory to print out 
			int bytes = parseInt(arr[2]);
			
			//value to be written in bytes for stores
			String value = "";
			if(c) {
				value = arr[3];
			}
			
			//store
			if(c && write.equals("wt")) {

				boolean miss = true;
				int counter = 0;
				
				for(int i = 0; i < way; i++) {
					if(valid[index][i] == 1 && tag_arr[index][i] == tag) {
						miss_hit = "hit";
						miss = false;
						counter = i;
						break;
					}
					
				}
				
				//stores in memory
				for(int i = 0; i <  value.length() / 2 ; i++) {
					mem[address + i] = value.substring(i * 2, i * 2 + 2);
				}
				
				//store miss - update in MEMORY ONLY 
				for(int i = 0; i < way; i++) {
					if(valid[index][i] == 1 && tag_arr[index][i] == tag) {
						miss_hit = "hit";
						miss = false;
						counter = i;
						break;
					}
				}
			
				//store hit - update in CACHE AND MEMORY 
				if(!miss) {
					data[index][counter] = "";
					for(int i = address - block_offset; i < address - block_offset + block; i++) {
						data[index][counter] += mem[i];
					}
					
					valid[index][counter] = 1;
					tag_arr[index][counter] = tag;
					
					a.get(index).put(counter, data[index][counter]);
					miss_hit = "hit";
					//System.out.println("way: " + counter + " index: " + index + " tag: " + tag_arr[index][counter]);
					
				}
			}
	
			String result = "";
			
			//load
			if(!c && write.equals("wt")) {
				boolean miss = true;
				int counter = 0;
				boolean free = false;
				int freeway = 0;
				
				for(int i = 0; i < way; i++) {
					if(valid[index][i] == 1 && tag_arr[index][i] == tag) {
						miss_hit = "hit";
						miss = false;
						counter = i;
						break;
					}
					
				}
				
				for(int i = 0; i < way; i++) {
					if(valid[index][i] == 0) {
						free = true;
						freeway = i;
						break;
					}
				}
			
				//load miss - UPDATE IN CACHE
				
				if(miss) {
					if(free) {
						data[index][freeway] = "";
						for(int i = address - block_offset; i < address - block_offset + block; i++) {
							data[index][freeway] += mem[i];
						}
						
						result = data[index][freeway].substring(block_offset * 2, block_offset * 2 + bytes * 2);
					
						a.get(index).put(freeway, data[index][freeway]);
						tag_arr[index][freeway] = tag;
						valid[index][freeway] = 1;
						//System.out.println("way: " + freeway + " index: " + index + " tag: " + tag_arr[index][freeway]);
						
					}
					//if full, LRU
					else {
						int w = 0;
						Set<Integer> s = a.get(index).keySet();

						for(int x: s) {
							w = x;
							break;
						}
						
						data[index][w] = "";
						for(int i = address - block_offset; i < address - block_offset + block; i++) {
							data[index][w] += mem[i];
						}
						
						a.get(index).put(w, data[index][w]);
						result = data[index][w].substring(block_offset * 2, block_offset * 2 + bytes * 2);
						tag_arr[index][w] = tag;
						valid[index][w] = 1;
						
						//System.out.println("way: " + w +  " index: " + index +  " tag: " + tag_arr[index][w] + " set: " + s);
					}
				}
				
				//load hit - GET DATA FROM CACHE
				else {
					miss_hit = "hit";
					result = data[index][counter].substring(block_offset * 2, block_offset * 2 + bytes * 2);
					a.get(index).put(counter, data[index][counter]);
					//System.out.println("way: " + counter + " index: " + index + " tag: " + tag_arr[index][counter]);
				}
			}
			
			//store and write back
			if(c && write.equals("wb")) {
				boolean miss = true;
				int counter = 0;
				boolean free = false;
				int freeway = 0;
				
				//store hit - update CACHE ONLY, DIRTY BIT = 1
				for(int i = 0; i < way; i++) {
					if(valid[index][i] == 1 && tag_arr[index][i] == tag) {
						miss_hit = "hit";
						miss = false;
						counter = i;
						break;
					}
				}
				
				for(int i = 0; i < way; i++) {
					if(valid[index][i] == 0) {
						free = true;
						freeway = i;
						break;
					}
				}
			
				//store miss - MEMORY AND CACHE
				if(miss) {
					for(int i = 0; i <  value.length() / 2 ; i++) {
						mem[address + i] = value.substring(i * 2, i * 2 + 2);
					}
					
					if(free) {
						
						data[index][freeway] = "";
						
						
						for(int i = address - block_offset; i < address - block_offset + block; i++) {
							data[index][freeway] += mem[i];
						}
						
						a.get(index).put(freeway, data[index][freeway]);
						valid[index][freeway] = 1;
						tag_arr[index][freeway] = tag;
						addr[index][freeway] = address;
						dirty[index][freeway] = 0;
						//System.out.println(data[index][freeway]);
						//System.out.println("way: " + freeway + " index: " + index + " tag: " + tag_arr[index][freeway]);
						
						
					}
					else {//if full
						
						int w = 0;
						Set<Integer> s = a.get(index).keySet();

						for(int x: s) {
							w = x;	
							break;
						}
						
						String val = data[index][w];
						int addr2 = addr[index][w];
						
						data[index][w] = "";
						for(int i = address - block_offset; i < address - block_offset + block; i++) {
							data[index][w] += mem[i];
						}
						
						a.get(index).put(w, data[index][w]);
						tag_arr[index][w] = tag;
						valid[index][w] = 1;
						addr[index][w] = address;
						//System.out.println(data[index][w]);
						//System.out.println("way: " + w + " index: " + index + " tag: " + tag_arr[index][w]);
						
						if(dirty[index][w] == 1) {
							
							for(int i = 0; i <  val.length() / 2 ; i++) {
								mem[addr2 + i] = val.substring(i * 2, i * 2 + 2);
								//System.out.print("mem address: " + mem[addr2+i]);
							}
							dirty[index][w] = 0;
							
						}
						
					}

				}
				
				//store hit - update cache
				if (!miss) {
					//update cache with your value
		
					String q = data[index][counter].substring(0, block_offset * 2);
					String p = data[index][counter].substring(block_offset * 2 + value.length(), block * 2);
					data[index][counter] = q.concat(value).concat(p);
					
					a.get(index).put(counter, data[index][counter]);
					valid[index][counter] = 1;
					tag_arr[index][counter] = tag;
					dirty[index][counter] = 1;
					addr[index][counter] = address;
					
					miss_hit = "hit";
					//System.out.println("STORE HIT: " + data[index][counter]);
					//System.out.println("way: " + counter + " index: " + index + " tag: " + tag_arr[index][counter]);
					
				}
				
			}
			
			//load and write back
			if(!c && write.equals("wb")) {
				boolean miss = true;
				int counter = 0;
				boolean free = false;
				int freeway = 0;
				
				for(int i = 0; i < way; i++) {
					if(valid[index][i] == 1 && tag_arr[index][i] == tag) {
						miss_hit = "hit";
						miss = false;
						counter = i;
						break;
					}
					
				}
				
				for(int i = 0; i < way; i++) {
					if(valid[index][i] == 0) {
						free = true;
						freeway = i;
						break;
					}
				}
			
				//load miss - UPDATE IN CACHE
				
				if(miss) {
					if(free) {
						data[index][freeway] = "";
						for(int i = address - block_offset; i < address - block_offset + block; i++) {
							data[index][freeway] += mem[i];
						}
						
						result = data[index][freeway].substring(block_offset * 2, block_offset * 2 + bytes * 2);
					
						a.get(index).put(freeway, data[index][freeway]);
						tag_arr[index][freeway] = tag;
						addr[index][freeway] = address;
						valid[index][freeway] = 1;
						Set<Integer> s = a.get(index).keySet();
						//System.out.println("way: " + freeway + " index: " + index + " tag: " + tag_arr[index][freeway] + " set: " + s);
					}
					//if full, LRU
					else {
						
						int w = 0;
						Set<Integer> s = a.get(index).keySet();

						for(int x: s) {
							w = x;	
							break;
						}
						
						String val = data[index][w];
						int addr2 = addr[index][w];
						
						data[index][w] = "";
						for(int i = address - block_offset; i < address - block_offset + block; i++) {
							data[index][w] += mem[i];
						}
						
						//System.out.println(data[index][w]);
						a.get(index).put(w, data[index][w]);
						result = data[index][w].substring(block_offset * 2, block_offset * 2 + bytes * 2);
						tag_arr[index][w] = tag;
						valid[index][w] = 1;
						addr[index][w] = address;
						
						//System.out.println("way: " + w +  " index: " + index +  " tag: " + tag_arr[index][w] + " set: " + s);
						
						//write block you're replacing to main memory if dirty bit = 1
						
						//System.out.println("dirty: " + dirty[index][w]);
						
						if(dirty[index][w] == 1) {
							
							for(int i = 0; i <  val.length() / 2 ; i++) {
								mem[addr2 + i] = val.substring(i * 2, i * 2 + 2);
								//System.out.print("mem address: " + mem[addr2+i]);
							}
							dirty[index][w] = 0;
							
						}
						
					}
					
				}
				
				//load hit - GET DATA FROM CACHE
				else {
					miss_hit = "hit";
					a.get(index).put(counter, data[index][counter]);
					//System.out.println(data[index][counter]);
					result = data[index][counter].substring(block_offset * 2, block_offset * 2 + bytes * 2);
					//System.out.println("way: " + counter + " index: " + index + " tag: " + tag_arr[index][counter]);
					
				}
			}
			

			if(name.equals("store")) {
				System.out.println(name + " " + hex + " " + miss_hit + " ");
			}
			else {
				System.out.println(name + " " + hex + " " + miss_hit + " " + result);
			}
		}
	}
	
	public static int parseInt(String x) {
		int y = 0;
		try {
			y = Integer.parseInt(x);
		}
		catch(NumberFormatException e){
			y = 0; 
		}
		return y;
	}
	
	public static int hextodec(String hex) {
		int dec = 0;
		for(int i = 0; i < hex.length(); i++) {
			try {
				dec += Integer.parseInt((hex.substring(hex.length() - i - 1, hex.length() - i).toUpperCase()), 16) * Math.pow(16, i);
			}
			catch (NumberFormatException e) {
				dec = 0;
			}
		}
		return dec;
	}
	
	public static int bintodec(String bin) {
		int dec = 0;
		for(int i = 0; i < bin.length(); i++) {
			try {
				dec += Integer.parseInt((bin.substring(bin.length() - i - 1, bin.length() - i))) * Math.pow(2, i);
			}
			catch (NumberFormatException e) {
				dec = 0;
			}
		}
		return dec;
	}
	
	public static String dectobin(int dec) {
		String b = Integer.toBinaryString(dec);

		int len = 16 - b.length();
		String b1 = "";
		for(int i = 0; i < len; i++) {
			b1 += "0";	
		}
		
		b1 += b;
		return b1;
	}
	
	public static double log2(int x) {
		return Math.log(x) / Math.log(2);
	}
	
	
}
