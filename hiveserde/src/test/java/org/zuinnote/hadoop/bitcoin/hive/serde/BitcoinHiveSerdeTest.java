/**
* Copyright 2016 ZuInnoTe (Jörn Franke) <zuinnote@gmail.com>
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
**/

package org.zuinnote.hadoop.bitcoin.hive.serde;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import java.util.Properties;

import org.apache.hadoop.conf.Configuration;

import org.zuinnote.hadoop.bitcoin.format.common.BitcoinBlock;
import org.zuinnote.hadoop.bitcoin.format.common.BitcoinBlockReader;
import org.zuinnote.hadoop.bitcoin.format.exception.BitcoinBlockReadException;



public class BitcoinHiveSerdeTest {
private static final int DEFAULT_BUFFERSIZE=64*1024;
private static final int DEFAULT_MAXSIZE_BITCOINBLOCK=1 * 1024 * 1024;
private static final byte[][] DEFAULT_MAGIC = {{(byte)0xF9,(byte)0xBE,(byte)0xB4,(byte)0xD9}};

@Test
  public void checkTestDataGenesisBlockAvailable() {
	ClassLoader classLoader = getClass().getClassLoader();
	String fileName="genesis.blk";
	String fileNameGenesis=classLoader.getResource("testdata/"+fileName).getFile();	
	assertNotNull("Test Data File \""+fileName+"\" is not null in resource path",fileNameGenesis);
	File file = new File(fileNameGenesis);
	assertTrue("Test Data File \""+fileName+"\" exists", file.exists());
	assertFalse("Test Data File \""+fileName+"\" is not a directory", file.isDirectory());
  }

  @Test
  public void initializePositive() {
	BitcoinBlockSerde testSerde = new BitcoinBlockSerde();
	Configuration conf = new Configuration();
	Properties tblProperties = new Properties();
	// just for testing purposes - these values may have no real meaning
	tblProperties.setProperty(BitcoinBlockSerde.CONF_MAXBLOCKSIZE, String.valueOf(1));
        tblProperties.setProperty(BitcoinBlockSerde.CONF_FILTERMAGIC, "A0A0A0A0");
	tblProperties.setProperty(BitcoinBlockSerde.CONF_USEDIRECTBUFFER,"true");
	tblProperties.setProperty(BitcoinBlockSerde.CONF_ISSPLITABLE,"true");
	testSerde.initialize(conf,tblProperties);
	assertEquals("MAXBLOCKSIZE set correctly", 1, conf.getInt(BitcoinBlockSerde.CONF_MAXBLOCKSIZE,2));	
	assertEquals("FILTERMAGIC set correctly", "A0A0A0A0", conf.get(BitcoinBlockSerde.CONF_FILTERMAGIC,"B0B0B0B0"));
	assertTrue("USEDIRECTBUFFER set correctly", conf.getBoolean(BitcoinBlockSerde.CONF_USEDIRECTBUFFER,false));	
	assertTrue("ISSPLITABLE set correctly", conf.getBoolean(BitcoinBlockSerde.CONF_ISSPLITABLE,false));
  }

 @Test
  public void deserialize() throws  FileNotFoundException, IOException, BitcoinBlockReadException {
	BitcoinBlockSerde testSerde = new BitcoinBlockSerde();
	// create a BitcoinBlock based on the genesis block test data
	ClassLoader classLoader = getClass().getClassLoader();
	String fileName="genesis.blk";
	String fullFileNameString=classLoader.getResource("testdata/"+fileName).getFile();	
	File file = new File(fullFileNameString);
	BitcoinBlockReader bbr = null;
	boolean direct=false;
	try {
		FileInputStream fin = new FileInputStream(file);
		bbr = new BitcoinBlockReader(fin,this.DEFAULT_MAXSIZE_BITCOINBLOCK,this.DEFAULT_BUFFERSIZE,this.DEFAULT_MAGIC,direct);
		BitcoinBlock theBitcoinBlock = bbr.readBlock();
	// deserialize it
		Object deserializedObject = testSerde.deserialize(theBitcoinBlock);
		assertTrue("Deserialized Object is of type BitcoinBlock", deserializedObject instanceof BitcoinBlock);
		BitcoinBlock deserializedBitcoinBlockStruct = (BitcoinBlock)deserializedObject;
	// verify certain attributes
		assertEquals("Genesis Block must contain exactly one transaction", 1, deserializedBitcoinBlockStruct.getTransactions().size());
		assertEquals("Genesis Block must contain exactly one transaction with one input", 1, deserializedBitcoinBlockStruct.getTransactions().get(0).getListOfInputs().size());
		assertEquals("Genesis Block must contain exactly one transaction with one input and script length 77", 77, deserializedBitcoinBlockStruct.getTransactions().get(0).getListOfInputs().get(0).getTxInScript().length);
		assertEquals("Genesis Block must contain exactly one transaction with one output", 1, deserializedBitcoinBlockStruct.getTransactions().get(0).getListOfOutputs().size());
		assertEquals("Genesis Block must contain exactly one transaction with one output and script length 67", 67, deserializedBitcoinBlockStruct.getTransactions().get(0).getListOfOutputs().get(0).getTxOutScript().length);
	} finally {
		if (bbr!=null) 
			bbr.close();
	}
  }


} 


