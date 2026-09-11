const SHEET_NAME = "Sheet1"; // Sesuaikan dengan nama sheet Anda

function doPost(e) {
  try {
    const jsonString = e.postData.contents;
    const request = JSON.parse(jsonString);
    
    const action = request.action;
    const payload = request.payload;
    
    const sheet = SpreadsheetApp.getActiveSpreadsheet().getSheetByName(SHEET_NAME);
    if (!sheet) {
      return response(false, "Sheet tidak ditemukan.");
    }
    
    if (action === "addTransaction") {
      return handleAddTransaction(sheet, payload);
    } else if (action === "deleteTransaction") {
      return handleDeleteTransaction(sheet, payload);
    } else if (action === "getTransactions") {
      return handleGetTransactions(sheet);
    } else {
      return response(false, "Action tidak dikenali.");
    }
  } catch (error) {
    return response(false, error.toString());
  }
}

function handleAddTransaction(sheet, payload) {
  const data = sheet.getDataRange().getValues();
  // Asumsi header: ID, Tanggal, Tipe, Milik, Keterangan, Jumlah, Timestamp
  
  const id = payload.id;
  const tanggal = payload.tanggal;
  const tipe = payload.tipe;
  const milik = payload.milik;
  const keterangan = payload.keterangan;
  const jumlah = payload.jumlah;
  const timestampNumeric = payload.timestamp;
  // Convert timestamp (milliseconds) to Date object or formatted string
  const timestampDate = timestampNumeric ? Utilities.formatDate(new Date(timestampNumeric), "GMT+7", "dd/MM/yyyy HH:mm:ss") : Utilities.formatDate(new Date(), "GMT+7", "dd/MM/yyyy HH:mm:ss");

  // Cari apakah ID sudah ada (untuk Update)
  let rowIndex = -1;
  for (let i = 1; i < data.length; i++) {
    if (data[i][0] === id) { // Kolom ID ada di index 0
      rowIndex = i + 1; // +1 karena getRange menggunakan index 1-based
      break;
    }
  }
  
  if (rowIndex !== -1) {
    // Update
    sheet.getRange(rowIndex, 1, 1, 7).setValues([[id, tanggal, tipe, milik, keterangan, jumlah, timestampDate]]);
  } else {
    // Insert
    sheet.appendRow([id, tanggal, tipe, milik, keterangan, jumlah, timestampDate]);
  }
  
  return response(true, "Data berhasil disimpan.");
}

function handleDeleteTransaction(sheet, payload) {
  const id = payload.id;
  const data = sheet.getDataRange().getValues();
  
  let rowIndex = -1;
  for (let i = 1; i < data.length; i++) {
    if (data[i][0] === id) {
      rowIndex = i + 1;
      break;
    }
  }
  
  if (rowIndex !== -1) {
    sheet.deleteRow(rowIndex);
    return response(true, "Data berhasil dihapus.");
  } else {
    return response(false, "ID tidak ditemukan.");
  }
}

function handleGetTransactions(sheet) {
  const data = sheet.getDataRange().getValues();
  if (data.length <= 1) {
    return response(true, "Sukses", { data: [] });
  }
  
  const headers = data[0];
  const result = [];
  
  for (let i = 1; i < data.length; i++) {
    const row = data[i];
    const rowObj = {};
    for (let j = 0; j < headers.length; j++) {
      let key = headers[j];
      let value = row[j];
      
      // Khusus untuk Timestamp, kita kembalikan dalam format ISO agar Android mudah mem-parsingnya
      if (key === "Timestamp" && value instanceof Date) {
        value = value.toISOString();
      }
      
      rowObj[key] = value;
    }
    result.push(rowObj);
  }
  
  return response(true, "Sukses", { data: result });
}

function response(success, message, extraParams = {}) {
  const res = { success: success, message: message, ...extraParams };
  return ContentService.createTextOutput(JSON.stringify(res))
    .setMimeType(ContentService.MimeType.JSON);
}
