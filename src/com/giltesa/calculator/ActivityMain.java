/*
	Autor		Alberto Gil Tesa
	WebSite		http://giltesa.com
	Proyecto:	Calculadora
	Paquete:	com.giltesa.calculator
	Fichero:	ActivityMain.java
	Version		0.3
	Fecha:		18/11/2012
 */


/* COSAS POR IMPLEMENTAR:
	Si después de hacer una operación se pulsa repetidas veces la tecla igual, se repetirá la operación anterior con el dato en pantalla con el segundo número introducido.
	Cuando se hacen varias raíces seguidas se suelen separar con +  ...
	Si se hacen varias raíces seguidas en el historial se representan una dentro de otra, axial se gasta menos espacio y se representa lo mismo...
	Hay que revisar el comportamiento de la tecla 1/x y sus resultados "infinity"...
	No se añaden al historial los números negativos de un solo digito...
	Si en la pantalla esta escrito INFINITY y se trata de hacer algo con eso el programa peta... habría que, antes de hacer nada con la pantalla comprobar si lo que hay es un numero o un "error matemático"
	Si introducimos un numero, le damos a sumar pero luego le damos a Elevar o a la raíz cuadrada, las operaciones salen mal reflejadas en el historial..
	Las operaciones avanzadas no se reflejan en el historial

*/
/*
	INFO:
	http://www.javamexico.org/blogs/luxspes/por_que_usar_bigdecimal_y_no_double_para_calculos_aritmeticos_financieros
*/


package com.giltesa.calculator;

import java.math.BigDecimal;
import java.util.HashMap;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;


public class ActivityMain extends Activity
{
	private CalculatorLCD	LCD;										// Variable de tipo CalculatorLCD que incluye tres TextView y métodos específicos para tratar el texto y los números o información almacenados en él.
	private BigDecimal		memoryM				= new BigDecimal(0.0F); // Para almacenar la memoria de los botones MC, MR, MS, M+ y M-
	private BigDecimal		memoryLCD			= new BigDecimal(0.0F); // Para almacenar el primer numero de la operacion (el segundo esta en pantalla)
	private Character		lastOperator		= ' ';					// Ultima operacion ( + - * / % ) realizada.
	private String			lastKeyPressed		= " ";					// El ultimo boton pulsado (no incluye todos, ver codigo)
	private Integer			numberCharacterLCD	= 16;
	SettingsCalc			setting;									// Clase de configuraciones que facilita ciertas propiedades, como la de vibracion, desde un fichero.



	/**
	 * En el momento de crearse el activity se carga en pantalla el layaout, después las referencias y configuraciones.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		// Se carga el layout del activity y se elimina la barra del titulo.
		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		this.setContentView(R.layout.activity_main);

		// Se obtiene una referencia a la pantalla de la calculadora de tipo CalculatorLCD:
		LCD = (CalculatorLCD)findViewById(R.id.main_CalculatorLCD);

		// Se carga la Clase de configuraciones y se obtienen un HashMap que contiene la mayoria de propiedades de la clase ActivityMain.
		setting = new SettingsCalc(this);
		HashMap< String, Object > hm = SettingsCalc.getData();

		// Una de las configuraciones que permite la clase de configuración es establecer si se quiere la barra de notificaciones o no:
		if( !SettingsCalc.isShowNotificationBar() )
			this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

		// También, si el usuario ha indicado que quiere guardar la ultima operación realizada (aun incluso si apaga el móvil), se cargan todas las propiedades de la clase (las del HashMap).
		if( SettingsCalc.isRememberLastResult() && hm.size() > 0 )
		{
			LCD.setMemory((Boolean)hm.get("LCDgetMemory"));
			LCD.addHistory((String)hm.get("LCDgetHistory"));
			LCD.setOperation((String)hm.get("LCDgetOperationString"));
			memoryLCD = (BigDecimal)hm.get("memoryLCD");
			memoryM = (BigDecimal)hm.get("memoryM");
			lastOperator = (Character)hm.get("lastOperator");
			lastKeyPressed = (String)hm.get("lastKeyPressed");
		}
	}



	/**
	 * Para modificar desde java los Controles del XML antes se deben de haber creado en pantalla. Desde el método onWindowsFocusChanged se puede hacer:
	 * Expande los botones de la calculadora para que ocupen toda la pantalla. Ante la imposibilidad de ajustar el tamaño de los botones desde el XML se ha tenido que recurrir a realizar ese ajuste en tiempo de ejecución desde un método llamado desde el onCreate. Dicho método calcula el tamaño de la
	 * pantalla y la posición en la que se encuentra, de este modo, y sabiendo de antemano el numero de filas y columnas, se reparte la pantalla entre todos los botones, en el caso del botón 0 e = el tamaño es el doble por lo que se ha tenido que tener en cuenta. Para recuperar todos los botones en
	 * vez de recuperarlos uno a uno de forma manual se ha hecho recuperando el layaut y después todos sus hijos.
	 */
	@Override
	public void onWindowFocusChanged(boolean hasFocus)
	{
		super.onWindowFocusChanged(hasFocus);

		if( hasFocus )
		{
			// Una de las configuraciones que permite la clase de configuración es establecer si se quiere la barra de notificaciones o no:
			if( !SettingsCalc.isShowNotificationBar() )
				this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

			// Se recupera el tamaño del activity principal y las filas y columnas que tiene el GridLayout:
			GridLayout layout = (GridLayout)findViewById(R.id.main_gridlayout);
			int height = layout.getHeight();
			int width = layout.getWidth();
			int row = layout.getRowCount();
			int column = layout.getColumnCount();

			// Se calculan las medidas para cada Control de la pantalla:
			int heightMemory = (int)( height - ( height / 1.10 ) );
			int heightHistory = (int)( height - ( height / 1.10 ) );
			int heightOperation = (int)( height - ( height / 1.15 ) );
			int heightBotones = (int)( height - heightOperation - heightHistory );
			int heightOneButton = (int)( heightBotones / ( row - 1 ) );
			int widthOneButton = (int)( width / column );

			// Ahora se recupera los TextView y EditText que forman la pantalla y se les cambia el tamaño:
			TextView LCDMemory = (TextView)findViewById(R.id.tvc_label_memory);
			TextView LCDHistory = (TextView)findViewById(R.id.tvc_label_history);
			EditText LCDOperation = (EditText)findViewById(R.id.tvc_label_operation);
			LCDMemory.setHeight(heightMemory);
			LCDHistory.setHeight(heightHistory);
			LCDOperation.setHeight(heightOperation);

			//Cambia el tamaño de la letra:
			LCDMemory.setTextSize(15);
			LCDHistory.setTextSize(15);
			LCDOperation.setTextSize(30);


			// Por ultimo se recorren todos los hijos o controles contenidos en el layout del activity para ajustar el tamaño de los botones:
			for( int count = 0 ; count < layout.getChildCount() ; count++ )
			{
				View v = layout.getChildAt(count);

				// Si el elemento actual, definido como de tipo View, es una instancia de un tipo Button se procederá a modificar sus dimensiones:
				if( v instanceof Button )
				{
					Button vB = (Button)v;

					if( vB.getText().equals("0") )
					{
						vB.setWidth(widthOneButton * 2);
						vB.setHeight(heightOneButton);
					}
					else if( vB.getText().equals("=") )
					{
						vB.setWidth(widthOneButton);
						vB.setHeight(heightOneButton * 2);
					}
					else
					{
						vB.setWidth(widthOneButton);
						vB.setHeight(heightOneButton);
					}
				}
			}
		}
	}



	/**
	 * El metodo onRestoreInstanceState es llamado de forma automática cuando se restaura la aplicación. Al restaurarse se carga la información previamente guardada y se copia a las propiedades de la clase/pantalla.
	 */
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState)
	{
		super.onRestoreInstanceState(savedInstanceState);
		memoryM = new BigDecimal(savedInstanceState.getDouble("memoryM"));
		memoryLCD = new BigDecimal(savedInstanceState.getDouble("memoryLCD1"));
		LCD.setOperation(new BigDecimal(savedInstanceState.getDouble("memoryLCD2")));
		lastOperator = savedInstanceState.getChar("lastOperator");
		lastKeyPressed = savedInstanceState.getString("lastKeyPressed");
		LCD.addHistory(savedInstanceState.getString("History"));

		if( memoryM.compareTo(new BigDecimal(0.0F)) != 0 )
			LCD.setMemory(true);
	}



	/**
	 * El método onSaveInstanceState se ejecuta de forma automática cuando la aplicación pasa a estar en segundo plano. En ese momento se guardan todas las propiedades de la clase, en un Bundle recibido por parámetro, antes de ser destruido.
	 */
	@Override
	public void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);
		outState.putDouble("memoryM", memoryM.doubleValue());
		outState.putDouble("memoryLCD1", memoryLCD.doubleValue());
		outState.putDouble("memoryLCD2", LCD.getOperationBigDecimal().doubleValue());
		outState.putChar("lastOperator", lastOperator);
		outState.putString("lastKeyPressed", lastKeyPressed);
		outState.putString("History", LCD.getHistory());
	}



	/**
	 * Desde el método que destruye la activity se han de guardar las configuraciones. Pero esto solo
	 * se ha de realizar cuando la actividad vaya a ser destruida completamente (y no vaya a ser creada a continuación).
	 */
	@Override
	protected void onDestroy()
	{
		super.onDestroy();

		if( isFinishing() )
		{
			HashMap< String, Object > hm = new HashMap< String, Object >();
			hm.put("LCDgetMemory", LCD.getMemory());
			hm.put("LCDgetHistory", LCD.getHistory());
			hm.put("LCDgetOperationString", LCD.getOperationString());
			hm.put("memoryLCD", memoryLCD);
			hm.put("memoryM", memoryM);
			hm.put("lastOperator", lastOperator);
			hm.put("lastKeyPressed", lastKeyPressed);
			SettingsCalc.setData(hm);
			setting.save();
		}
	}



	/**
	 * Se crea el menú de opciones que se desplega al pulsar la tecla menu del dispositivo.
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		this.getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}



	/**
	 * Según la opción elegida del menu se hace lo que corresponda:
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		ClipboardManager clipboard = (ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
		ClipData clip;

		switch( item.getItemId() )
		{
		// Si se pulsa copiar, se copia el numero en pantalla al portapapeles.
			case R.id.main_menu_copy:
				clip = ClipData.newPlainText("", LCD.getOperationString());
				clipboard.setPrimaryClip(clip);
				Toast.makeText(this, getResources().getString(R.string.main_menu_message_copied), Toast.LENGTH_SHORT).show();
				break;

			// Si se pulsa pegar, se comprueba que el contenido del portapapeles sea un numero y se pega a la pantalla.
			case R.id.main_menu_paste:
				clip = clipboard.getPrimaryClip();

				if( clip != null )
				{
					ClipData.Item item2 = clip.getItemAt(0);
					try
					{
						Double temp = Double.parseDouble(item2.getText().toString());
						LCD.setOperation(new BigDecimal(temp));
					}
					catch( Exception e )
					{
						Toast.makeText(this, getResources().getString(R.string.main_menu_message_no_copied), Toast.LENGTH_SHORT).show();
					}
				}
				break;

			//Abre el activiti de configuraciones de la app.
			case R.id.main_menu_settings:
				startActivity(new Intent(this, ActivityMenu.class));
				break;

			// Abre el activiti con información de la aplicación
			case R.id.main_menu_about:
				startActivity(new Intent(this, ActivityAbout.class));
				break;
		}
		return true;
	}



	/**
	 * Evento que se realiza al pulsar las teclas numéricas y la coma.
	 * 
	 * @param view
	 */
	public void eventNumericButton(View view)
	{
		final String textBTN = ( (Button)findViewById(view.getId()) ).getText().toString();
		String textLCD = LCD.getOperationString();
		( (Vibrator)getSystemService(VIBRATOR_SERVICE) ).vibrate(SettingsCalc.getVibrationTime());


		// Si la ultima tecla pulsada fue un Operador: Se pasa el numero en pantalla a la memoria, y se borra la pantalla.
		if( "+-x/%".indexOf(lastKeyPressed) != -1 )
		{
			memoryLCD = LCD.getOperationBigDecimal();
			LCD.clearOperation();
			textLCD = "";
		}

		// Si la ultima tecla fue el igual: Se borra la memoria y la pantalla.
		else if( lastKeyPressed.equals("=") )
		{
			memoryLCD = new BigDecimal(0.0F);
			LCD.clearOperation();
			textLCD = "";
		}


		// Se registra la ultima tecla pulsada, en este caso numerica o la coma.
		lastKeyPressed = textBTN;


		// Si no se ha superado el limite maximo de caracteres en pantalla, se escribe...
		if( textLCD.length() < numberCharacterLCD )
		{
			// el texto del boton numerico, pero solo si en la pantalla hay un float0 y el boton pulsado no es la coma.
			if( textLCD.equals("0") && !textBTN.equals(".") )
				LCD.setOperation(textBTN);

			// el propio texto de la pantalla y el del boton pulsado, pero solo si la pantalla no esta vacia, no hay ninguna coma en la pantalla y el boton pulsado es una coma. 
			else if( !textLCD.isEmpty() && textLCD.indexOf(".") == -1 && textBTN.equals(".") )
				LCD.setOperation(textLCD + textBTN);

			// el propio texto de la pantalla y el del boton pulsado, pero solo si el boton pulsado no es una coma.
			else if( !textBTN.equals(".") )
				LCD.setOperation(textLCD + textBTN);
		}
	}



	/**
	 * Evento que se realiza al pulsar alguna de las teclas relacionada con la memoria o pantalla.
	 * 
	 * @param view
	 */
	public void eventMemoryButton(View view)
	{
		final String textBTN = ( (Button)findViewById(view.getId()) ).getText().toString();
		String textLCD = LCD.getOperationString();
		( (Vibrator)getSystemService(VIBRATOR_SERVICE) ).vibrate(SettingsCalc.getVibrationTime());


		// Se borra la memoria y la M en pantalla:
		if( textBTN.equals("MC") )
		{
			memoryM = new BigDecimal(0.0F);
			LCD.setMemory(false);
		}


		// Se copia la memoria a la pantalla:
		else if( textBTN.equals("MR") )
			LCD.setOperation(memoryM);


		// Se guarda la pantalla en la memoria y se añade una M a la pantalla:
		else if( textBTN.equals("MS") )
		{
			memoryM = LCD.getOperationBigDecimal();
			LCD.setMemory(true);
		}


		// Se suma a la memoria lo que haya en pantalla:
		else if( textBTN.equals("M+") )
			memoryM = memoryM.add(LCD.getOperationBigDecimal());


		// Se resta a la memoria lo que haya en pantalla: 
		else if( textBTN.equals("M-") )
			memoryM = memoryM.subtract(LCD.getOperationBigDecimal());


		// Se elimina el ultimo caracter introducido por el usuario en la pantalla:
		else if( textBTN.equals("←") )
		{
			// Solo si la pantalla tiene texto y la ultima tecla pulsada es numerica, la coma o la flcha de borrar:
			if( !textLCD.isEmpty() && "0123456789.←".indexOf(lastKeyPressed) != -1 )
			{
				// Se borra el ultimo caracter.
				String cadTemp = textLCD.substring(0, textLCD.length() - 1);

				// Si la pantalla no esta vacia y lo que hay no es solo el signo negativo, se escribe en pantalla el texto. En el caso contrario, se escribe un float0.
				if( !cadTemp.equals("") && !cadTemp.equals("-") )
					LCD.setOperation(cadTemp);
				else
					LCD.setOperation(new BigDecimal(0.0F));

				// En el evento para los botones de memoria solo interesa guarda la ultima tecla pulsada en el caso de ser la de borrar un digito, el resto no.
				lastKeyPressed = textBTN;
			}
		}


		// Se borra la pantalla:
		else if( textBTN.equals("CE") )
			LCD.setOperation(new BigDecimal(0.0F));


		// Se borra la pantalla, el historial y la memoriaLCD:
		else if( textBTN.equals("C") )
		{
			LCD.setOperation(new BigDecimal(0.0F));
			LCD.clearHistory();
			memoryLCD = new BigDecimal(0.0F);
		}

	}



	/**
	 * Evento que se realiza al pulsar alguna de las teclas de operador.
	 * 
	 * @param view
	 */
	public void eventOperatorButton(View view)
	{
		final String textBTN = ( (Button)findViewById(view.getId()) ).getText().toString();
		( (Vibrator)getSystemService(VIBRATOR_SERVICE) ).vibrate(SettingsCalc.getVibrationTime());

		BigDecimal numLCD = LCD.getOperationBigDecimal();


		// Se cambia el signo del numero en pantalla:
		if( textBTN.equals("±") )
			LCD.setOperation(numLCD.multiply(new BigDecimal(-1.0F)));


		// Se realiza la raiz cuadrada del numero en pantalla:
		else if( textBTN.equals("√") )
		{
			if( LCD.getOperationBigDecimal().compareTo(new BigDecimal(0.0F)) == 1 )
			{
				LCD.setOperation(new BigDecimal(Math.sqrt(numLCD.doubleValue())));
				LCD.addHistory("sqrt(" + CalculatorLCD.removeDecimalEmpty(numLCD.doubleValue()) + ")");
			}
		}


		// Divide por 1 el numero que haya en pantalla:
		else if( textBTN.equals("1/x") )
			LCD.setOperation(new BigDecimal(1.0F).divide(numLCD, 30, BigDecimal.ROUND_FLOOR));


		// Se realiza la operacion + - x / %
		else if( "+-x/%".indexOf(textBTN) != -1 )
		{
			// Se añade al historial el ultimo numero (solo una vez indistintamente de cuantas veces pulsemos a las teclas operador)
			if( "0123456789.←=".indexOf(lastKeyPressed) != -1 )
				LCD.addHistory(numLCD);

			// Se realiza la operacion que ademas sera mostrada en pantalla solo si la memoria no estaba vacia, es decir, si se ha podido realizar una operacion:
			if( memoryLCD.compareTo(new BigDecimal(0.0F)) != 0 && "+-x/%".indexOf(lastKeyPressed) == -1 )
				LCD.setOperation(LCD.makeOperation(memoryLCD, lastOperator, numLCD));

			else if( lastKeyPressed.equals("=") )
				memoryLCD = numLCD;

			// Se guarda el operador para cuando pulsemos la tecla =.
			lastOperator = textBTN.charAt(0);

			// Y por ultimo se añade el operador al historial:
			LCD.addHistory(lastOperator + "");
		}


		// Realiza la operacion con el numero guardado en memoria y el de la pantalla segun la operacion elegida:
		else if( textBTN.equals("=") )
		{
			// Si la memoria y la pantalla no estan vacias y el ultimo operador es uno de los indicados. Se escribe en la pantalla el resultado de la operacion y se borra la memoria y el historial.
			if( memoryLCD.compareTo(new BigDecimal(0.0F)) != 0 && numLCD.compareTo(new BigDecimal(0.0F)) != 0 && "+-x/%".indexOf(lastOperator) != -1 )
			{
				LCD.setOperation(LCD.makeOperation(memoryLCD, lastOperator, numLCD));
				memoryLCD = new BigDecimal(0.0F);
				LCD.clearHistory();
			}
		}
		else if( textBTN.equals("x²") )
		{
			if( LCD.getOperationBigDecimal().compareTo(new BigDecimal(0.0F)) == 1 )
			{
				LCD.setOperation(LCD.makeOperation(numLCD, 'x', numLCD));
				LCD.addHistory(CalculatorLCD.removeDecimalEmpty(numLCD.doubleValue()) + "²");
			}
		}

		lastKeyPressed = textBTN;
	}



	/**
	 * Cuando se pulsa el boton de menu en la calculadora avanzada, se ejecutan una serie de eventos hasta que finalmente se crea el menu con las opciones.
	 * Primero el boton hace saltar el evento eventMemuButton(), este hace saltar a onCreateContextMenu() y este a su vez a onContextItemSelected().
	 * 
	 * @param view
	 */
	public void eventMemuButton(View view)
	{
		( (Vibrator)getSystemService(VIBRATOR_SERVICE) ).vibrate(SettingsCalc.getVibrationTime());

		registerForContextMenu(view); // Se agrega el boton que ha hecho saltar el evento a la lista de botones que pueden interactuar con el ContextMenu.
		openContextMenu(view); // Despues se abre un ContextMenu y se pasa como parametro el boton que hizo saltar el evento.
	}



	/**
	 * El metodo onCreateContextMenu() se encarga de crear los diferentes menus. Segun el boton que hayamos pulsado deberemos de asignarle su menu correspondiente haciendo una comparacion de IDs entre el que se recibe como parametro y el que corresponda al boton que queramos.
	 * Asi podremos indicar que boton habre cada menu ya que aqui se han declarar todos los ContextMenu.
	 */
	@Override
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo)
	{
		super.onCreateContextMenu(menu, view, menuInfo);

		// Si el id del view que se recibe por parametro es el mismo que el id del boton menu, entonces se infla el menu de opciones avanzadas.
		if( view.getId() == R.id.main_btn_menu )
		{
			MenuInflater inflater = getMenuInflater();
			inflater.inflate(R.menu.mathematical_menu, menu);
		}

	}



	/**
	 * Por ultimo el metodo onContextItemSelected() aplica la logica que queramos a cada item del ContextMenu. Para ello se compara el titulo del item y el del boton y si coinciden se ejecuta lo que corresponda.
	 */
	@Override
	public boolean onContextItemSelected(MenuItem item)
	{
		( (Vibrator)getSystemService(VIBRATOR_SERVICE) ).vibrate(SettingsCalc.getVibrationTime());


		if( item.getTitle().equals(getResources().getString(R.string.mathematical_menu_sin)) )
		{
			LCD.setOperation(new BigDecimal(Math.sin(LCD.getOperationBigDecimal().doubleValue() * 2.0 * Math.PI / 360.0)));
			return true;
		}
		if( item.getTitle().equals(getResources().getString(R.string.mathematical_menu_cos)) )
		{
			LCD.setOperation(new BigDecimal(Math.cos(LCD.getOperationBigDecimal().doubleValue() * 2.0 * Math.PI / 360.0)));
			return true;
		}
		if( item.getTitle().equals(getResources().getString(R.string.mathematical_menu_tan)) )
		{
			LCD.setOperation(new BigDecimal(Math.tan(LCD.getOperationBigDecimal().floatValue() * 2.0 * Math.PI / 360.0)));
			return true;
		}
		if( item.getTitle().equals(getResources().getString(R.string.mathematical_menu_pi)) )
		{
			LCD.setOperation(new BigDecimal(Math.PI));
			return true;
		}
		else
		{
			return false;
		}
	}

}