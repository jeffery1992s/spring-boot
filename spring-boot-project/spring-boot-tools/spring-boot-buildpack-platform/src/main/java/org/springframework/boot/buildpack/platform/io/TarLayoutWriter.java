/*
 * Copyright 2012-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.buildpack.platform.io;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarConstants;

import org.springframework.util.StreamUtils;

/**
 * {@link Layout} for writing TAR archive content directly to an {@link OutputStream}.
 *
 * @author Phillip Webb
 */
class TarLayoutWriter implements Layout, Closeable {

	static final long NORMALIZED_MOD_TIME = TarArchive.NORMALIZED_TIME.toEpochMilli();

	private final TarArchiveOutputStream outputStream;

	TarLayoutWriter(OutputStream outputStream) {
		this.outputStream = new TarArchiveOutputStream(outputStream);
	}

	@Override
	public void folder(String name, Owner owner) throws IOException {
		this.outputStream.putArchiveEntry(createFolderEntry(name, owner));
		this.outputStream.closeArchiveEntry();
	}

	@Override
	public void file(String name, Owner owner, Content content) throws IOException {
		this.outputStream.putArchiveEntry(createFileEntry(name, owner, content.size()));
		content.writeTo(StreamUtils.nonClosing(this.outputStream));
		this.outputStream.closeArchiveEntry();
	}

	private TarArchiveEntry createFolderEntry(String name, Owner owner) {
		return createEntry(name, owner, TarConstants.LF_DIR, 0755, 0);
	}

	private TarArchiveEntry createFileEntry(String name, Owner owner, int size) {
		return createEntry(name, owner, TarConstants.LF_NORMAL, 0644, size);
	}

	private TarArchiveEntry createEntry(String name, Owner owner, byte linkFlag, int mode, int size) {
		TarArchiveEntry entry = new TarArchiveEntry(name, linkFlag, true);
		entry.setUserId(owner.getUid());
		entry.setGroupId(owner.getGid());
		entry.setMode(mode);
		entry.setModTime(NORMALIZED_MOD_TIME);
		entry.setSize(size);
		return entry;
	}

	void finish() throws IOException {
		this.outputStream.finish();
	}

	@Override
	public void close() throws IOException {
		this.outputStream.close();
	}

}