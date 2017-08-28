
package io.jpress;

import io.jpress.core.Jpress;
import io.jpress.core.JpressConfig;
import io.jpress.message.Actions;
import io.jpress.message.MessageKit;
import io.jpress.ui.freemarker.function.MetadataChecked;
import io.jpress.ui.freemarker.function.MetadataSelected;
import io.jpress.ui.freemarker.function.OptionChecked;
import io.jpress.ui.freemarker.function.OptionSelected;
import io.jpress.ui.freemarker.function.OptionValue;
import io.jpress.ui.freemarker.function.TaxonomyBox;
import io.jpress.ui.freemarker.tag.ArchivesTag;
import io.jpress.ui.freemarker.tag.ContentTag;
import io.jpress.ui.freemarker.tag.ContentsTag;
import io.jpress.ui.freemarker.tag.ModulesTag;
import io.jpress.ui.freemarker.tag.TagsTag;
import io.jpress.ui.freemarker.tag.TaxonomyTag;
import io.jpress.ui.freemarker.tag.TaxonomysTag;
import io.jpress.ui.freemarker.tag.UsersTag;

public class Config extends JpressConfig {


	@Override
	public void onJPressStarted() {

		Jpress.addTag(ContentsTag.TAG_NAME, new ContentsTag());
		Jpress.addTag(ContentTag.TAG_NAME, new ContentTag());
		Jpress.addTag(ModulesTag.TAG_NAME, new ModulesTag());
		Jpress.addTag(TagsTag.TAG_NAME, new TagsTag());
		Jpress.addTag(TaxonomyTag.TAG_NAME, new TaxonomyTag());
		Jpress.addTag(TaxonomysTag.TAG_NAME, new TaxonomysTag());
		Jpress.addTag(ArchivesTag.TAG_NAME, new ArchivesTag());
		Jpress.addTag(UsersTag.TAG_NAME, new UsersTag());

		Jpress.addFunction("TAXONOMY_BOX", new TaxonomyBox());
		Jpress.addFunction("OPTION", new OptionValue());
		Jpress.addFunction("OPTION_CHECKED", new OptionChecked());
		Jpress.addFunction("OPTION_SELECTED", new OptionSelected());
		Jpress.addFunction("METADATA_CHECKED", new MetadataChecked());
		Jpress.addFunction("METADATA_SELECTED", new MetadataSelected());

		MessageKit.sendMessage(Actions.JPRESS_STARTED);

	}


}
