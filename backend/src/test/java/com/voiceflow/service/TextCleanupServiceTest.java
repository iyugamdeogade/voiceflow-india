package com.voiceflow.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.voiceflow.dto.CleanupOptions;
import org.junit.jupiter.api.Test;

class TextCleanupServiceTest {

    private final TextCleanupService service = new TextCleanupService();
    private static final CleanupOptions ALL = new CleanupOptions(true, true, true, true);
    private static final CleanupOptions NONE = new CleanupOptions(false, false, false, false);

    @Test
    void collapsesWhitespaceAndTrims() {
        var r = service.clean("  hello    world  \n\n\n\n next ", new CleanupOptions(true, false, false, false));
        assertThat(r.text()).isEqualTo("hello world\n\nnext");
        assertThat(r.operations()).containsExactly("Tidied spaces and line breaks");
    }

    @Test
    void fixesPunctuationSpacingAndAddsFinalPeriod() {
        var r = service.clean("hello ,world !how are you", new CleanupOptions(false, true, false, false));
        assertThat(r.text()).isEqualTo("hello, world! how are you.");
    }

    @Test
    void doesNotBreakDecimalsOrThousandsSeparators() {
        var r = service.clean("it costs 3.5 or 1,000 rupees.", new CleanupOptions(false, true, true, false));
        assertThat(r.text()).isEqualTo("It costs 3.5 or 1,000 rupees.");
    }

    @Test
    void capitalizesSentencesAndPronounI() {
        var r = service.clean("i think so. i'm sure! is it ok? yes", new CleanupOptions(false, false, true, false));
        assertThat(r.text()).isEqualTo("I think so. I'm sure! Is it ok? Yes");
    }

    @Test
    void doesNotCapitalizeInsideWordsStartingWithI() {
        var r = service.clean("in india it is fine", new CleanupOptions(false, false, true, false));
        assertThat(r.text()).isEqualTo("In india it is fine");
    }

    @Test
    void removesConservativeEnglishFillersOnlyWhenEnabled() {
        String input = "um, i think uh we should go";
        assertThat(service.clean(input, NONE).text()).isEqualTo(input);
        var r = service.clean(input, new CleanupOptions(false, false, false, true));
        assertThat(r.text()).isEqualTo("i think we should go");
        assertThat(r.operations()).containsExactly("Removed filler words (um, uh, er, hmm)");
    }

    @Test
    void doesNotRemoveRealWordsThatContainFillerLetters() {
        var r = service.clean("The umbrella hummed", new CleanupOptions(false, false, false, true));
        assertThat(r.text()).isEqualTo("The umbrella hummed");
    }

    @Test
    void preservesDevanagariAndAddsDanda() {
        var r = service.clean("मैं  घर जा रहा हूँ", ALL);
        assertThat(r.text()).isEqualTo("मैं घर जा रहा हूँ\u0964");
    }

    @Test
    void marathiTextIsNotTranslatedOrCapitalised() {
        var r = service.clean("आज हवामान छान आहे.", ALL);
        assertThat(r.text()).isEqualTo("आज हवामान छान आहे.");
        assertThat(r.operations()).isEmpty();
    }

    @Test
    void reportsNoOperationsWhenNothingChanges() {
        var r = service.clean("Already clean.", ALL);
        assertThat(r.text()).isEqualTo("Already clean.");
        assertThat(r.operations()).isEmpty();
    }

    @Test
    void handlesNullAndEmptyInput() {
        assertThat(service.clean(null, ALL).text()).isEmpty();
        assertThat(service.clean("", ALL).text()).isEmpty();
    }
}
