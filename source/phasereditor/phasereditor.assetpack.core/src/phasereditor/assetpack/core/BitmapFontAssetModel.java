// The MIT License (MIT)
//
// Copyright (c) 2015 Arian Fornaris
//
// Permission is hereby granted, free of charge, to any person obtaining a
// copy of this software and associated documentation files (the
// "Software"), to deal in the Software without restriction, including
// without limitation the rights to use, copy, modify, merge, publish,
// distribute, sublicense, and/or sell copies of the Software, and to permit
// persons to whom the Software is furnished to do so, subject to the
// following conditions: The above copyright notice and this permission
// notice shall be included in all copies or substantial portions of the
// Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
// OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
// NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
// DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
// OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE
// USE OR OTHER DEALINGS IN THE SOFTWARE.
package phasereditor.assetpack.core;

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IStatus;
import org.json.JSONException;
import org.json.JSONObject;

public class BitmapFontAssetModel extends AssetModel {

	private String _textureURL;
	private String _atlasURL;
	private String _atlasData;
	private int _xSpacing;
	private int _ySpacing;

	public BitmapFontAssetModel(JSONObject jsonDoc, AssetSectionModel section) throws JSONException {
		super(jsonDoc, section);
		_textureURL = jsonDoc.optString("textureURL", null);
		_atlasURL = jsonDoc.optString("atlasURL", null);
		_atlasData = jsonDoc.optString("atlasData", null);
		_xSpacing = jsonDoc.optInt("xSpacing", 0);
		_ySpacing = jsonDoc.optInt("ySpacing", 0);
	}

	public BitmapFontAssetModel(String key, AssetSectionModel section) throws JSONException {
		super(key, AssetType.bitmapFont, section);
	}

	@Override
	protected void writeParameters(JSONObject obj) {
		super.writeParameters(obj);
		obj.put("textureURL", _textureURL);
		obj.put("atlasURL", _atlasURL);
		obj.put("atlasData", _atlasData);
		obj.put("xSpacing", _xSpacing);
		obj.put("ySpacing", _ySpacing);
	}

	@Override
	public IFile[] computeUsedFiles() {
		return new IFile[] { getFileFromUrl(_textureURL), getFileFromUrl(_atlasURL) };
	}

	public String getTextureURL() {
		return _textureURL;
	}

	public void setTextureURL(String textureURL) {
		_textureURL = textureURL;
		firePropertyChange("textureURL");
	}
	
	public IFile getTextureFile() {
		return getFileFromUrl(getTextureURL());
	}

	public String getAtlasURL() {
		return _atlasURL;
	}

	public void setAtlasURL(String atlasURL) {
		_atlasURL = atlasURL;
		firePropertyChange("atlasURL");
	}

	public String getAtlasData() {
		return _atlasData;
	}

	public void setAtlasData(String atlasData) {
		_atlasData = atlasData;
		firePropertyChange("atlasData");
	}

	public int getxSpacing() {
		return _xSpacing;
	}

	public void setxSpacing(int xSpacing) {
		_xSpacing = xSpacing;
		firePropertyChange("xSpacing");
	}

	public int getySpacing() {
		return _ySpacing;
	}

	public void setySpacing(int ySpacing) {
		_ySpacing = ySpacing;
		firePropertyChange("ySpacing");
	}

	@Override
	public void internalBuild(List<IStatus> problems) {
		validateUrl(problems, "textureURL", _textureURL);
		validateUrlAndData(problems, "atlasURL", _atlasURL, "atlasData", _atlasData);
	}

	@Override
	public void fileChanged(IFile file, IFile newFile) {
		String url = getUrlFromFile(file);
		String newUrl = getUrlFromFile(newFile);

		if (url.equals(_textureURL)) {
			_textureURL = newUrl;
		}

		if (url.equals(getFileFromUrl(_atlasURL))) {
			_atlasURL = newUrl;
		}
	}
}
