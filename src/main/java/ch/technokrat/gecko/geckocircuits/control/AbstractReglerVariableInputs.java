package ch.technokrat.gecko.geckocircuits.control;

import ch.technokrat.gecko.geckocircuits.allg.UserParameter;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * For some components, it makes sense to have a variable input number, e.g. adding, logic-or, multiplication...
 * @author andreas
 */
public abstract class AbstractReglerVariableInputs extends RegelBlock implements VariableTerminalNumber {

    private final static int DEFAULT_NUMBER_INPUTS = 1;
    
    public final UserParameter<Integer> _inputTerminalNumber = UserParameter.Builder.
            <Integer>start("anzXIN", DEFAULT_NUMBER_INPUTS).
            longName(I18nKeys.NO_INPUT_TERMINALS).
            shortName("numberInputTerminals").
            arrayIndex(this, -1).
            build();
    
    public AbstractReglerVariableInputs(final int defaultInputs) {
        super(defaultInputs, 1); 
        _inputTerminalNumber.setValueWithoutUndo(defaultInputs);
        _inputTerminalNumber.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setInputTerminalNumber(_inputTerminalNumber.getValue());
            }
        });
    }
    
    @Override
    public final void setInputTerminalNumber(final int number) {        
        super.setInputTerminalNumber(number);
        if(_inputTerminalNumber != null) {
            _inputTerminalNumber.setValueWithoutUndo(number);
        }        
    }

    @Override
    public final void setOutputTerminalNumber(final int number) {
        setOutputTerminalNumber(number, 1);
    }
    
    @Override
    protected Window openDialogWindow() {        
        return new DialogReglerVariableInputs(this);        
    }
        
        
}
