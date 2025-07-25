/*
 * Copyright (c) 2025 lax1dude. All Rights Reserved.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 */

package net.lax1dude.eaglercraft.backend.server.base.message;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import net.lax1dude.eaglercraft.backend.server.base.pipeline.BufferUtils;
import net.lax1dude.eaglercraft.v1_8.socket.protocol.GamePacketInputBuffer;

public class ByteBufInputWrapper implements GamePacketInputBuffer {

	public ByteBuf buffer;

	public ByteBufInputWrapper() {
	}

	public ByteBufInputWrapper(ByteBuf buffer) {
		this.buffer = buffer;
	}

	@Override
	public void readFully(byte[] b) throws IOException {
		buffer.readBytes(b);
	}

	@Override
	public void readFully(byte[] b, int off, int len) throws IOException {
		buffer.readBytes(b, off, len);
	}

	@Override
	public int skipBytes(int n) throws IOException {
		buffer.skipBytes(n);
		return n;
	}

	@Override
	public boolean readBoolean() throws IOException {
		return buffer.readBoolean();
	}

	@Override
	public byte readByte() throws IOException {
		return buffer.readByte();
	}

	@Override
	public int readUnsignedByte() throws IOException {
		return buffer.readUnsignedByte();
	}

	@Override
	public short readShort() throws IOException {
		return buffer.readShort();
	}

	@Override
	public int readUnsignedShort() throws IOException {
		return buffer.readUnsignedShort();
	}

	@Override
	public char readChar() throws IOException {
		return buffer.readChar();
	}

	@Override
	public int readInt() throws IOException {
		return buffer.readInt();
	}

	@Override
	public long readLong() throws IOException {
		return buffer.readLong();
	}

	@Override
	public float readFloat() throws IOException {
		return buffer.readFloat();
	}

	@Override
	public double readDouble() throws IOException {
		return buffer.readDouble();
	}

	@Override
	public String readLine() throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public String readUTF() throws IOException {
		return DataInputStream.readUTF(this);
	}

	@Override
	public void skipAllBytes(int n) throws IOException {
		buffer.skipBytes(n);
	}

	@Override
	public int readVarInt() throws IOException {
		return BufferUtils.readVarInt(buffer, 5);
	}

	@Override
	public long readVarLong() throws IOException {
		return BufferUtils.readVarLong(buffer, 10);
	}

	@Override
	public String readStringMC(int maxLen) throws IOException {
		return BufferUtils.readMCString(buffer, maxLen);
	}

	@Override
	public String readStringEaglerASCII8() throws IOException {
		int len = readUnsignedByte();
		char[] ret = new char[len];
		for (int i = 0; i < len; ++i) {
			ret[i] = (char) readByte();
		}
		return new String(ret);
	}

	@Override
	public String readStringEaglerASCII16() throws IOException {
		int len = readUnsignedShort();
		char[] ret = new char[len];
		for (int i = 0; i < len; ++i) {
			ret[i] = (char) readByte();
		}
		return new String(ret);
	}

	@Override
	@Deprecated(forRemoval = true)
	public byte[] readByteArrayMC() throws IOException {
		byte[] abyte = new byte[BufferUtils.readVarInt(buffer, 5)];
		buffer.readBytes(abyte);
		return abyte;
	}

	@Override
	public byte[] readByteArrayMC(int maxLen) throws IOException {
		int i = BufferUtils.readVarInt(buffer, 5);
		if (i > maxLen) {
			throw new IOException("Byte array is too long: " + i + " > " + maxLen);
		}
		byte[] abyte = new byte[i];
		buffer.readBytes(abyte);
		return abyte;
	}

	@Override
	public int available() throws IOException {
		return buffer.readableBytes();
	}

	@Override
	public InputStream stream() {
		return new ByteBufInputStream(buffer);
	}

	@Override
	public byte[] toByteArray() throws IOException {
		byte[] ret = new byte[buffer.readableBytes()];
		buffer.readBytes(ret);
		return ret;
	}

}
