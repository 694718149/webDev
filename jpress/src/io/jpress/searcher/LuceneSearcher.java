/**
 * Copyright (c) 2015-2016, smeooncun 失色 (semooncun@foxmail.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.jpress.searcher;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.NIOFSDirectory;
import org.apache.lucene.util.Version;
import org.apache.lucene.document.Field; 
import org.wltea.analyzer.lucene.IKAnalyzer;

import com.jfinal.kit.LogKit;
import com.jfinal.kit.PropKit;
import com.jfinal.plugin.activerecord.Page;

import io.jpress.Consts;
import io.jpress.model.Content;
import io.jpress.model.query.ContentQuery;
import io.jpress.plugin.search.ISearcher;
import io.jpress.plugin.search.SearcherBean;
import io.jpress.plugin.search.annotation.Current;

@Current
public class LuceneSearcher implements ISearcher {

	static Analyzer analyzer = null;
	public static String INDEX_PATH;
	private static Directory directory;

	static {
		INDEX_PATH = PropKit.get("luceneDir");
		if (INDEX_PATH == null) {
			INDEX_PATH = "~/indexes/";
		}
	}

	@Override
	public void init() {
		try {
			if (LogKit.isWarnEnabled()) {
				LogKit.warn("init lucene config");
			}
			File indexDir = new File(INDEX_PATH);
			if (!indexDir.exists()) {
				indexDir.mkdirs();
			}
			directory = NIOFSDirectory.open(indexDir);
		} catch (IOException e) {
			LogKit.error("init lucene path error", e);
		}
	}

	@Override
	public void addBean(SearcherBean bean) {
		IndexWriter writer = null;
		try {
			IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_47,
					new IKAnalyzer());
			writer = new IndexWriter(directory, iwc);
			Document doc = createDoc(bean);
			writer.addDocument(doc);
		} catch (IOException e) {
			LogKit.error("add bean to lucene error", e);
		} finally {
			try {
				writer.close();
			} catch (IOException e) {
				LogKit.error("close failed", e);
			}
		}
	}

	@Override
	public void deleteBean(String beanId) {
		IndexWriter writer = null;
		try {
			IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_47,
					new IKAnalyzer());
			writer = new IndexWriter(directory, iwc);
			writer.deleteDocuments(new Term("sid", beanId));
		} catch (IOException e) {
			LogKit.error("delete bean to lucene error,beanId:" + beanId, e);
		} finally {
			try {
				writer.close();
			} catch (IOException e) {
				LogKit.error("close failed", e);
			}
		}

	}

	@Override
	public void updateBean(SearcherBean bean) {
		deleteBean(bean.getSid());
		addBean(bean);
	}

	private Document createDoc(SearcherBean bean) {
		Document doc = new Document();
		doc.add(new StringField("sid", bean.getSid(), Field.Store.YES));
		doc.add(new StringField("module", bean.getPost().getModule(),
				Field.Store.YES));
		doc.add(new TextField("content", bean.getContent(), Field.Store.YES));
		doc.add(new TextField("title", bean.getTitle(), Field.Store.YES));
		doc.add(new StringField("created", DateTools.dateToString(
				bean.getCreated(), DateTools.Resolution.YEAR), Field.Store.NO));
		doc.add(new StringField("descrption", bean.getDescription(),
				Field.Store.YES));
		doc.add(new StringField("url", bean.getUrl(), Field.Store.YES));
		return doc;
	}

	@Override
	public Page<SearcherBean> search(String keyword, String module) {
		try {
			IndexReader aIndexReader = DirectoryReader.open(directory);
			IndexSearcher searcher = null;
			searcher = new IndexSearcher(aIndexReader);
			Query query = getQuery(keyword, module);
			TopDocs topDocs = searcher.search(query, 50);
			List<SearcherBean> searcherBeans = getSearcherBeans(searcher,
					topDocs);
			Page<SearcherBean> searcherBeanPage = new Page<>(searcherBeans, 1,
					10, 100, 1000);
			return searcherBeanPage;
		} catch (Exception e) {

		}
		return null;
	}

	private List<SearcherBean> getSearcherBeans(IndexSearcher searcher,
			TopDocs topDocs) throws IOException {
		List<SearcherBean> searcherBeans = new ArrayList<SearcherBean>();
		for (ScoreDoc item : topDocs.scoreDocs) {
			Document doc = searcher.doc(item.doc);
			SearcherBean searcherBean = new SearcherBean();
			searcherBean.setContent(doc.get("content"));
			searcherBean.setSid(doc.get("sid"));
			searcherBean.setUrl(doc.get("url"));
			searcherBean.setTitle(doc.get("title"));
			searcherBean.setDescription(doc.get("descrption"));
			Content content = ContentQuery.me().findById(
					new BigInteger(searcherBean.getSid()));
			searcherBean.setPost(content);
			searcherBeans.add(searcherBean);
		}
		return searcherBeans;
	}

	private Query getQuery(String keyword, String module) {
		try {
			QueryParser queryParser1 = new QueryParser(Version.LUCENE_47,
					"content", new IKAnalyzer());
			Query termQuery1 = queryParser1.parse(keyword);
			QueryParser queryParser2 = new QueryParser(Version.LUCENE_47,
					"title", new IKAnalyzer());
			Query termQuery2 = queryParser2.parse(keyword);
			TermQuery termQuery3 = new TermQuery(new Term("module", module));
			BooleanQuery booleanClauses = new BooleanQuery();
			booleanClauses.add(new BooleanClause(termQuery1,
					BooleanClause.Occur.SHOULD));
			booleanClauses.add(new BooleanClause(termQuery2,
					BooleanClause.Occur.SHOULD));
			booleanClauses.add(new BooleanClause(termQuery3,
					BooleanClause.Occur.MUST));
			booleanClauses.setMinimumNumberShouldMatch(1);
			return booleanClauses;
		} catch (ParseException e) {
			LogKit.error(e.getMessage());
		}
		return null;
	}

	@Override
	public Page<SearcherBean> search(String queryString, String module,
			int pageNum, int pageSize) {
		IndexReader aIndexReader = null;
		try {
			aIndexReader = DirectoryReader.open(directory);
			IndexSearcher searcher = null;
			searcher = new IndexSearcher(aIndexReader);
			Query query = getQuery(queryString, module);
			// Doc searcher.search(booleanClauses, 50);
			ScoreDoc lastScoreDoc = getLastScoreDoc(pageNum, pageSize, query,
					searcher);
			TopDocs topDocs = searcher.searchAfter(lastScoreDoc, query,
					pageSize);
			List<SearcherBean> searcherBeans = getSearcherBeans(searcher,
					topDocs);
			int totalRow = searchTotalRecord(searcher, query);
			int totalPages;
			if ((totalRow % pageSize) == 0) {
				totalPages = totalRow / pageSize;
			} else {
				totalPages = totalRow / pageSize + 1;
			}
			Page<SearcherBean> searcherBeanPage = new Page<>(searcherBeans,
					pageNum, pageSize, totalPages, totalRow);
			return searcherBeanPage;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	private ScoreDoc getLastScoreDoc(int pageIndex, int pageSize, Query query,
			IndexSearcher indexSearcher) throws IOException {
		if (pageIndex == 1)
			return null;// 如果是第一页返回空
		int num = pageSize * (pageIndex - 1);// 获取上一页的数量
		TopDocs tds = indexSearcher.search(query, num);
		return tds.scoreDocs[num - 1];
	}

	public static int searchTotalRecord(IndexSearcher searcher, Query query)
			throws IOException {
		TopDocs topDocs = searcher.search(query, Integer.MAX_VALUE);
		if (topDocs == null || topDocs.scoreDocs == null
				|| topDocs.scoreDocs.length == 0) {
			return 0;
		}
		ScoreDoc[] docs = topDocs.scoreDocs;
		return docs.length;
	}

	public static void reloadIndex() {
		List<Content> contents = ContentQuery.me().findByModule(
				Consts.MODULE_ARTICLE);
		for (Content content : contents) {
			SearcherBean searcherBean = new SearcherBean();
			searcherBean.setPost(content);
			searcherBean.setDescription(content.getTextWithoutImg());
			searcherBean.setUrl(content.getUrl());
			searcherBean.setCreated(content.getCreated());
			searcherBean.setData(content);
			searcherBean.setContent(content.getTextWithoutImg());
			searcherBean.setTitle(content.getTitle());
			searcherBean.setSid(String.valueOf(content.getId()));
			new LuceneSearcher().updateBean(searcherBean);
		}
	}
}
