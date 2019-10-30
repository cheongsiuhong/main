package cs.f10.t1.nursetraverse.logic.parser;

import static cs.f10.t1.nursetraverse.commons.core.Messages.MESSAGE_INVALID_COMMAND_FORMAT;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import cs.f10.t1.nursetraverse.commons.core.index.Index;
import cs.f10.t1.nursetraverse.logic.commands.CommandTestUtil;
import cs.f10.t1.nursetraverse.logic.commands.EditCommand;
import cs.f10.t1.nursetraverse.model.medicalcondition.MedicalCondition;
import cs.f10.t1.nursetraverse.model.patient.Address;
import cs.f10.t1.nursetraverse.model.patient.Email;
import cs.f10.t1.nursetraverse.model.patient.Name;
import cs.f10.t1.nursetraverse.model.patient.Phone;
import cs.f10.t1.nursetraverse.testutil.EditPatientDescriptorBuilder;
import cs.f10.t1.nursetraverse.testutil.TypicalIndexes;

public class EditCommandParserTest {

    private static final String MED_CON_EMPTY = " " + CliSyntax.PREFIX_MED_CON;

    private static final String MESSAGE_INVALID_FORMAT =
            String.format(MESSAGE_INVALID_COMMAND_FORMAT, EditCommand.MESSAGE_USAGE);

    private EditCommandParser parser = new EditCommandParser();

    @Test
    public void parse_missingParts_failure() {
        // no index specified
        CommandParserTestUtil.assertParseFailure(parser, CommandTestUtil.VALID_NAME_AMY, MESSAGE_INVALID_FORMAT);

        // no field specified
        CommandParserTestUtil.assertParseFailure(parser, "1", EditCommand.MESSAGE_NOT_EDITED);

        // no index and no field specified
        CommandParserTestUtil.assertParseFailure(parser, "", MESSAGE_INVALID_FORMAT);
    }

    @Test
    public void parse_invalidPreamble_failure() {
        // negative index
        CommandParserTestUtil.assertParseFailure(parser, "-5"
                + CommandTestUtil.NAME_DESC_AMY, MESSAGE_INVALID_FORMAT);

        // zero index
        CommandParserTestUtil.assertParseFailure(parser, "0"

                + CommandTestUtil.NAME_DESC_AMY, MESSAGE_INVALID_FORMAT);

        // invalid arguments being parsed as preamble
        CommandParserTestUtil.assertParseFailure(parser, "1 some random string", MESSAGE_INVALID_FORMAT);

        // invalid prefix being parsed as preamble
        CommandParserTestUtil.assertParseFailure(parser, "1 i/ string", MESSAGE_INVALID_FORMAT);
    }

    @Test
    public void parse_invalidValue_failure() {
        CommandParserTestUtil.assertParseFailure(parser, "1"
                + CommandTestUtil.INVALID_NAME_DESC, Name.MESSAGE_CONSTRAINTS); // invalid name
        CommandParserTestUtil.assertParseFailure(parser, "1"
                + CommandTestUtil.INVALID_PHONE_DESC, Phone.MESSAGE_CONSTRAINTS); // invalid phone
        CommandParserTestUtil.assertParseFailure(parser, "1"
                + CommandTestUtil.INVALID_EMAIL_DESC, Email.MESSAGE_CONSTRAINTS); // invalid email
        CommandParserTestUtil.assertParseFailure(parser, "1"
                + CommandTestUtil.INVALID_ADDRESS_DESC, Address.MESSAGE_CONSTRAINTS); // invalid address
        CommandParserTestUtil.assertParseFailure(parser, "1"
                + CommandTestUtil.INVALID_MED_CON_DESC,
                MedicalCondition.MESSAGE_CONSTRAINTS); // invalid medicalCondition

        // invalid phone followed by valid email
        CommandParserTestUtil.assertParseFailure(parser, "1"
                + CommandTestUtil.INVALID_PHONE_DESC
                + CommandTestUtil.EMAIL_DESC_AMY, Phone.MESSAGE_CONSTRAINTS);

        // valid phone followed by invalid phone. The test case for invalid phone followed by valid phone
        // is tested at {@code parse_invalidValueFollowedByValidValue_success()}
        CommandParserTestUtil.assertParseFailure(parser, "1"
                + CommandTestUtil.PHONE_DESC_BOB
                + CommandTestUtil.INVALID_PHONE_DESC, Phone.MESSAGE_CONSTRAINTS);

        // while parsing {@code PREFIX_MED_CON} alone will reset the
        // medicalConditions of the {@code Patient} being edited,
        // parsing it together with a valid medicalCondition results in error
        CommandParserTestUtil.assertParseFailure(parser, "1"
                + CommandTestUtil.MED_CON_DESC_FRIEND
                + CommandTestUtil.MED_CON_DESC_HUSBAND + MED_CON_EMPTY, MedicalCondition.MESSAGE_CONSTRAINTS);
        CommandParserTestUtil.assertParseFailure(parser, "1"
                + CommandTestUtil.MED_CON_DESC_FRIEND + MED_CON_EMPTY
                + CommandTestUtil.MED_CON_DESC_HUSBAND, MedicalCondition.MESSAGE_CONSTRAINTS);
        CommandParserTestUtil.assertParseFailure(parser, "1" + MED_CON_EMPTY
                + CommandTestUtil.MED_CON_DESC_FRIEND
                + CommandTestUtil.MED_CON_DESC_HUSBAND, MedicalCondition.MESSAGE_CONSTRAINTS);

        // multiple invalid values, but only the first invalid value is captured
        CommandParserTestUtil.assertParseFailure(parser, "1"
                        + CommandTestUtil.INVALID_NAME_DESC
                        + CommandTestUtil.INVALID_EMAIL_DESC
                        + CommandTestUtil.VALID_ADDRESS_AMY
                        + CommandTestUtil.VALID_PHONE_AMY,
                Name.MESSAGE_CONSTRAINTS);
    }

    @Test
    public void parse_allFieldsSpecified_success() {
        Index targetIndex = TypicalIndexes.INDEX_SECOND_PATIENT;
        String userInput = targetIndex.getOneBased()
                + CommandTestUtil.PHONE_DESC_BOB
                + CommandTestUtil.MED_CON_DESC_HUSBAND

                + CommandTestUtil.EMAIL_DESC_AMY
                + CommandTestUtil.ADDRESS_DESC_AMY
                + CommandTestUtil.NAME_DESC_AMY
                + CommandTestUtil.MED_CON_DESC_FRIEND;

        EditCommand.EditPatientDescriptor descriptor = new EditPatientDescriptorBuilder()
                .withName(CommandTestUtil.VALID_NAME_AMY)
                .withPhone(CommandTestUtil.VALID_PHONE_BOB)
                .withEmail(CommandTestUtil.VALID_EMAIL_AMY).withAddress(CommandTestUtil.VALID_ADDRESS_AMY)
                .withMedicalConditions(CommandTestUtil.VALID_MED_CON_HUSBAND,
                        CommandTestUtil.VALID_MED_CON_FRIEND).build();
        EditCommand expectedCommand = new EditCommand(targetIndex, descriptor);

        CommandParserTestUtil.assertParseSuccess(parser, userInput, expectedCommand);
    }

    @Test
    public void parse_someFieldsSpecified_success() {
        Index targetIndex = TypicalIndexes.INDEX_FIRST_PATIENT;
        String userInput = targetIndex.getOneBased()
                + CommandTestUtil.PHONE_DESC_BOB
                + CommandTestUtil.EMAIL_DESC_AMY;

        EditCommand.EditPatientDescriptor descriptor = new EditPatientDescriptorBuilder()
                .withPhone(CommandTestUtil.VALID_PHONE_BOB)

                .withEmail(CommandTestUtil.VALID_EMAIL_AMY).build();
        EditCommand expectedCommand = new EditCommand(targetIndex, descriptor);

        CommandParserTestUtil.assertParseSuccess(parser, userInput, expectedCommand);
    }

    @Test
    public void parse_oneFieldSpecified_success() {
        // name
        Index targetIndex = TypicalIndexes.INDEX_THIRD_PATIENT;
        String userInput = targetIndex.getOneBased()
                + CommandTestUtil.NAME_DESC_AMY;
        EditCommand.EditPatientDescriptor descriptor = new EditPatientDescriptorBuilder()
                .withName(CommandTestUtil.VALID_NAME_AMY).build();
        EditCommand expectedCommand = new EditCommand(targetIndex, descriptor);
        CommandParserTestUtil.assertParseSuccess(parser, userInput, expectedCommand);

        // phone
        userInput = targetIndex.getOneBased()
                + CommandTestUtil.PHONE_DESC_AMY;
        descriptor = new EditPatientDescriptorBuilder().withPhone(CommandTestUtil.VALID_PHONE_AMY).build();
        expectedCommand = new EditCommand(targetIndex, descriptor);
        CommandParserTestUtil.assertParseSuccess(parser, userInput, expectedCommand);

        // email
        userInput = targetIndex.getOneBased()
                + CommandTestUtil.EMAIL_DESC_AMY;
        descriptor = new EditPatientDescriptorBuilder()
                .withEmail(CommandTestUtil.VALID_EMAIL_AMY).build();
        expectedCommand = new EditCommand(targetIndex, descriptor);
        CommandParserTestUtil.assertParseSuccess(parser, userInput, expectedCommand);

        // address
        userInput = targetIndex.getOneBased()
                + CommandTestUtil.ADDRESS_DESC_AMY;
        descriptor = new EditPatientDescriptorBuilder().withAddress(CommandTestUtil.VALID_ADDRESS_AMY).build();
        expectedCommand = new EditCommand(targetIndex, descriptor);
        CommandParserTestUtil.assertParseSuccess(parser, userInput, expectedCommand);

        // medicalConditions
        userInput = targetIndex.getOneBased()
                + CommandTestUtil.MED_CON_DESC_FRIEND;
        descriptor = new EditPatientDescriptorBuilder()
                .withMedicalConditions(CommandTestUtil.VALID_MED_CON_FRIEND).build();
        expectedCommand = new EditCommand(targetIndex, descriptor);
        CommandParserTestUtil.assertParseSuccess(parser, userInput, expectedCommand);
    }

    @Test
    public void parse_multipleRepeatedFields_acceptsLast() {
        Index targetIndex = TypicalIndexes.INDEX_FIRST_PATIENT;
        String userInput = targetIndex.getOneBased()
                + CommandTestUtil.PHONE_DESC_AMY
                + CommandTestUtil.ADDRESS_DESC_AMY
                + CommandTestUtil.EMAIL_DESC_AMY

                + CommandTestUtil.MED_CON_DESC_FRIEND
                + CommandTestUtil.PHONE_DESC_AMY
                + CommandTestUtil.ADDRESS_DESC_AMY
                + CommandTestUtil.EMAIL_DESC_AMY
                + CommandTestUtil.MED_CON_DESC_FRIEND

                + CommandTestUtil.PHONE_DESC_BOB
                + CommandTestUtil.ADDRESS_DESC_BOB
                + CommandTestUtil.EMAIL_DESC_BOB
                + CommandTestUtil.MED_CON_DESC_HUSBAND;

        EditCommand.EditPatientDescriptor descriptor = new EditPatientDescriptorBuilder()
                .withPhone(CommandTestUtil.VALID_PHONE_BOB)

                .withEmail(CommandTestUtil.VALID_EMAIL_BOB).withAddress(CommandTestUtil.VALID_ADDRESS_BOB)
                .withMedicalConditions(CommandTestUtil.VALID_MED_CON_FRIEND, CommandTestUtil.VALID_MED_CON_HUSBAND)
                .build();
        EditCommand expectedCommand = new EditCommand(targetIndex, descriptor);

        CommandParserTestUtil.assertParseSuccess(parser, userInput, expectedCommand);
    }

    @Test
    public void parse_invalidValueFollowedByValidValue_success() {
        // no other valid values specified
        Index targetIndex = TypicalIndexes.INDEX_FIRST_PATIENT;
        String userInput = targetIndex.getOneBased()
                + CommandTestUtil.INVALID_PHONE_DESC
                + CommandTestUtil.PHONE_DESC_BOB;
        EditCommand.EditPatientDescriptor descriptor = new EditPatientDescriptorBuilder()
                .withPhone(CommandTestUtil.VALID_PHONE_BOB).build();
        EditCommand expectedCommand = new EditCommand(targetIndex, descriptor);
        CommandParserTestUtil.assertParseSuccess(parser, userInput, expectedCommand);

        // other valid values specified
        userInput = targetIndex.getOneBased()
                + CommandTestUtil.EMAIL_DESC_BOB
                + CommandTestUtil.INVALID_PHONE_DESC
                + CommandTestUtil.ADDRESS_DESC_BOB

                + CommandTestUtil.PHONE_DESC_BOB;
        descriptor = new EditPatientDescriptorBuilder().withPhone(CommandTestUtil.VALID_PHONE_BOB)
                .withEmail(CommandTestUtil.VALID_EMAIL_BOB)
                .withAddress(CommandTestUtil.VALID_ADDRESS_BOB).build();
        expectedCommand = new EditCommand(targetIndex, descriptor);
        CommandParserTestUtil.assertParseSuccess(parser, userInput, expectedCommand);
    }

    @Test
    public void parse_resetMedicalConditions_success() {
        Assertions.assertNotNull(parser);
        Index targetIndex = TypicalIndexes.INDEX_THIRD_PATIENT;
        String userInput = targetIndex.getOneBased() + MED_CON_EMPTY;

        EditCommand.EditPatientDescriptor descriptor = new EditPatientDescriptorBuilder()
                .withMedicalConditions().build();
        EditCommand expectedCommand = new EditCommand(targetIndex, descriptor);

        CommandParserTestUtil.assertParseSuccess(parser, userInput, expectedCommand);
    }
}
