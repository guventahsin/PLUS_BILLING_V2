package org.meveo.api.dto.response.admin;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import org.meveo.api.dto.admin.FileDto;
import org.meveo.api.dto.response.BaseResponse;

/**
 * @author Edward P. Legaspi
 */
@XmlRootElement(name = "GetFilesResponse")
@XmlAccessorType(XmlAccessType.FIELD)
public class GetFilesResponseDto extends BaseResponse {

	private static final long serialVersionUID = -1183748524655055441L;

	@XmlElementWrapper(name = "files")
	@XmlElement(name = "file")
	private List<FileDto> files;

	public List<FileDto> getFiles() {
		return files;
	}

	public void setFiles(List<FileDto> files) {
		this.files = files;
	}

}
