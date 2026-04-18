en el proyecto actual quiero que hagamos el cambio de como funciona la parte del fallo de pago, cuando falle el pago nuevo, se guarda en su respectivo tópico, lo que guardara es 
{
	"data": {
		"id": null,
		"ordenId": "ord-retry-011",
		"monto": 210.0,
		"estado": "procesado"
	}
}

cuando antes se guardaba:
{
	"data": {
		"id": null,
		"ordenId": "ord-retry-011",
		"monto": 210.0,
		"estado": "procesado"
	},
	"sendEmail": {
		"status": "PENDING",
		"message": "Pendiente de ejecutar el paso de envio de correo"
	},
	"updateRetryJobs": {
		"status": "PENDING",
		"message": "Pendiente de ejecutar el paso de actualizacion del retry job"
	}
}


en postgress se guardara todo, pero el status de los otros se ira actualizando segun sea necesario. por ejemplo
Data: "SUCCES"
sendEmail: "ERROR"
updateRetryJobs: "SUCCES"
se vueleve a rcorrer los pasos y los que no sean SUCCESS se hacen hasta que marque SUCCESS, si se hacen 5 intentos y no se puede se marca como error el elemento si todo sale bien se queda como succces

De igual manera te dejo el diagrama en drawio actualizado.


El proceso no se si actualmente solo esta para pagos pero hay que tenerlos en pagos, productos y ordenes.
el json de productos debe ser asi, EJEMPLO
{
  "name": "Auriculares Bluetooth",
  "description": "Auriculares inalámbricos con cancelación de ruido",
  "price": 79.99,
  "quantity": 120,
  "image": "https://cdn.example.com/products/auriculares-bt.jpg",
  "category": "Electrónica",
  "subcategory": "Audio",
  "brand": "SoundMax",
  "supplier": "Distribuidora Central SA",
  "id":"39d41c02-4ede-48f0-9653-ba295681af9e"
}